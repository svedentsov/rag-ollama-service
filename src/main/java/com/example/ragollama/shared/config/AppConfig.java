package com.example.ragollama.shared.config;

import com.example.ragollama.shared.config.properties.AppProperties;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.netty.http.client.HttpClient;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Основной конфигурационный класс приложения.
 * <p>
 * Содержит централизованную настройку ключевых бинов, таких как
 * пулы потоков и HTTP-клиенты, обеспечивая консистентность и надежность во всем приложении.
 */
@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final AppProperties appProperties;

    /**
     * Включает автоматическую передачу контекста Reactor в другие контексты,
     * такие как MDC, при старте приложения.
     */
    @PostConstruct
    void initializeReactorContext() {
        Hooks.enableAutomaticContextPropagation();
    }

    /**
     * Создает и настраивает основной, переиспользуемый ObjectMapper для всего приложения.
     * Эта конфигурация делает десериализацию Enum нечувствительной к регистру и
     * добавляет поддержку современных типов даты/времени Java 8 (JSR-310).
     *
     * @return Сконфигурированный ObjectMapper.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build()
                .findAndRegisterModules();
    }

    /**
     * Создает и настраивает основной, переиспользуемый строитель для {@link WebClient}.
     * <p>
     * Эта конфигурация является центральной точкой для всех HTTP-взаимодействий. Она
     * устанавливает таймауты, пулы соединений и использует кастомный {@link ObjectMapper},
     * что обеспечивает консистентность и надежность всех внешних вызовов.
     *
     * @param objectMapper ObjectMapper, настроенный для всего приложения.
     * @return Сконфигурированный {@link WebClient.Builder}.
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder(ObjectMapper objectMapper) {
        final var httpClientProps = appProperties.httpClient();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) httpClientProps.connectTimeout().toMillis())
                .responseTimeout(httpClientProps.responseTimeout())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(httpClientProps.readWriteTimeout().toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(httpClientProps.readWriteTimeout().toSeconds(), TimeUnit.SECONDS)));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                });
    }

    /**
     * Создает основной пул потоков для общих фоновых задач.
     *
     * @return Сконфигурированный {@link AsyncTaskExecutor}.
     */
    @Bean
    @Primary
    public AsyncTaskExecutor applicationTaskExecutor() {
        return createExecutor(appProperties.taskExecutor(), "app-async-");
    }

    /**
     * Создает выделенный пул потоков для долгих I/O-операций с LLM.
     *
     * @return Изолированный {@link AsyncTaskExecutor} для LLM.
     */
    @Bean
    public AsyncTaskExecutor llmTaskExecutor() {
        return createExecutor(appProperties.llmExecutor(), "llm-async-");
    }

    /**
     * Создает выделенный пул потоков для быстрых I/O-операций с базой данных.
     *
     * @return Изолированный {@link AsyncTaskExecutor} для БД.
     */
    @Bean
    public AsyncTaskExecutor databaseTaskExecutor() {
        return createExecutor(appProperties.dbExecutor(), "db-async-");
    }

    /**
     * Вспомогательный метод для создания и конфигурации {@link ThreadPoolTaskExecutor}.
     *
     * @param props  Конфигурация пула.
     * @param prefix Префикс для имен потоков.
     * @return Готовый к использованию {@link AsyncTaskExecutor}.
     */
    private AsyncTaskExecutor createExecutor(AppProperties.TaskExecutor props, String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.corePoolSize());
        executor.setMaxPoolSize(props.maxPoolSize());
        executor.setQueueCapacity(props.queueCapacity());
        executor.setThreadNamePrefix(prefix);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * Декоратор, который копирует MDC-контекст из родительского потока в дочерний.
     * Это обеспечивает сквозную трассировку по `requestId` в асинхронных операциях.
     */
    static class MdcTaskDecorator implements TaskDecorator {
        /**
         * Оборачивает {@link Runnable}, копируя в него MDC-контекст.
         *
         * @param runnable Исходная задача.
         * @return Обернутая задача с сохраненным контекстом.
         */
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }

    /**
     * Создает планировщик для библиотеки Resilience4j.
     * <p>
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
