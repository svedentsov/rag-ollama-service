package com.example.ragollama.config;

import com.example.ragollama.config.properties.AppProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Основной конфигурационный класс приложения.
 * <p>
 * В этой версии конфигурация дополнена автоматической передачей контекста
 * (например, MDC) в асинхронные потоки Project Reactor с помощью библиотеки
 * `micrometer-context-propagation`. Это обеспечивает сквозную трассировку
 * запросов в логах без необходимости ручной настройки декораторов.
 */
@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final AppProperties appProperties;

    /**
     * Инициализирует глобальный хук Project Reactor для автоматического
     * проброса контекста (MDC) во все реактивные цепочки.
     * <p>
     * Этот метод вызывается один раз при старте приложения и делает
     * контекст доступным во всех операторах, таких как map, flatMap,
     * doOnNext и т.д., даже при переключении потоков.
     */
    @PostConstruct
    void initializeReactorContext() {
        Hooks.enableAutomaticContextPropagation();
    }

    /**
     * Создает и настраивает главный бин {@link WebClient.Builder} для всех HTTP-взаимодействий.
     * Этот метод не создает конечный {@link WebClient}, а настраивает и возвращает
     * {@link WebClient.Builder}, который затем может быть внедрен в другие компоненты
     * (например, в конфигурацию Spring AI) для создания экземпляров WebClient с единой, централизованной конфигурацией.
     * Конфигурация является production-ready, так как включает все необходимые таймауты для обеспечения отказоустойчивости.
     *
     * @return Сконфигурированный и готовый к использованию {@link WebClient.Builder}.
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        final var httpClientProps = appProperties.httpClient();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) httpClientProps.connectTimeout().toMillis())
                .responseTimeout(httpClientProps.responseTimeout())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(httpClientProps.readWriteTimeout().toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(httpClientProps.readWriteTimeout().toSeconds(), TimeUnit.SECONDS)));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    /**
     * Создает единый пул потоков для всех асинхронных задач.
     * Конфигурация пула полностью управляется через {@link AppProperties},
     * что позволяет гибко настраивать его без изменения кода.
     * <p>
     * <b>Примечание:</b> Ручной {@code MdcTaskDecorator} больше не нужен,
     * так как библиотека {@code micrometer-context-propagation} автоматически
     * оборачивает все бины типа {@code TaskExecutor} для проброса контекста.
     *
     * @return Сконфигурированный и управляемый Spring'om {@link AsyncTaskExecutor}.
     */
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        final var executorProps = appProperties.taskExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(executorProps.corePoolSize());
        executor.setMaxPoolSize(executorProps.maxPoolSize());
        executor.setQueueCapacity(executorProps.queueCapacity());
        executor.setThreadNamePrefix(executorProps.threadNamePrefix());
        executor.initialize();
        return executor;
    }

    /**
     * Создает планировщик для библиотеки Resilience4j.
     * Этот планировщик используется компонентом TimeLimiter для асинхронного
     * прерывания операций по таймауту. Выделение отдельного потока
     * обеспечивает изоляцию и предсказуемость работы механизмов отказоустойчивости.
     *
     * @return Экземпляр {@link ScheduledExecutorService} с одним потоком.
     */
    @Bean(name = "resilience4jScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService resilience4jScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("resilience-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }
}
