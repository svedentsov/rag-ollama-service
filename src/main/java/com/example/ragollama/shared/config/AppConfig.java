package com.example.ragollama.shared.config;

import com.example.ragollama.shared.config.properties.AppProperties;
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
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Основной конфигурационный класс приложения.
 * <p>
 * В этой версии конфигурация пула потоков обернута в {@link DelegatingSecurityContextAsyncTaskExecutor}
 * для корректного проброса контекста Spring Security в асинхронные задачи.
 */
@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final AppProperties appProperties;

    @PostConstruct
    void initializeReactorContext() {
        Hooks.enableAutomaticContextPropagation();
    }

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
     * Создает единый пул потоков для всех асинхронных задач с поддержкой Security Context.
     * <p>
     * Базовый {@link ThreadPoolTaskExecutor} оборачивается в {@link DelegatingSecurityContextAsyncTaskExecutor}.
     * Этот декоратор гарантирует, что {@code SecurityContext} из вызывающего потока
     * (например, HTTP-потока) будет автоматически передан в поток, выполняющий
     * задачу, аннотированную {@code @Async}.
     *
     * @return Сконфигурированный и безопасный для Spring Security {@link AsyncTaskExecutor}.
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
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
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
