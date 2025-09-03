package com.example.ragollama.shared.config;

import com.example.ragollama.shared.config.properties.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.netty.http.client.HttpClient;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Основной конфигурационный класс приложения.
 *
 * <p>В этой версии конфигурация пула потоков дополнена {@link TaskDecorator}
 * для автоматического проброса контекста логирования (MDC) в асинхронные задачи,
 * что обеспечивает сквозную трассировку по `requestId`. Также WebClient настраивается
 * с кастомным ObjectMapper.
 */
@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    /**
     * Включает автоматическую передачу контекста Reactor в другие контексты,
     * такие как MDC, при старте приложения.
     */
    @PostConstruct
    void initializeReactorContext() {
        Hooks.enableAutomaticContextPropagation();
    }

    /**
     * Создает и настраивает основной, переиспользуемый строитель для {@link WebClient}.
     *
     * <p>Эта конфигурация является центральной точкой для всех HTTP-взаимодействий. Она
     * устанавливает таймауты, пулы соединений и использует кастомный {@link ObjectMapper},
     * что обеспечивает консистентность и надежность всех внешних вызовов.
     *
     * @return Сконфигурированный {@link WebClient.Builder}.
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
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                });
    }

    /**
     * Создает единый пул потоков для всех асинхронных задач с поддержкой Security Context и MDC.
     *
     * <p>Базовый {@link ThreadPoolTaskExecutor} оборачивается в декораторы, которые
     * гарантируют, что и {@code SecurityContext}, и контекст логирования {@code MDC}
     * будут автоматически переданы из вызывающего потока в поток, выполняющий задачу.
     *
     * @return Сконфигурированный и безопасный для Spring Security и логирования {@link AsyncTaskExecutor}.
     */
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        final var executorProps = appProperties.taskExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(executorProps.corePoolSize());
        executor.setMaxPoolSize(executorProps.maxPoolSize());
        executor.setQueueCapacity(executorProps.queueCapacity());
        executor.setThreadNamePrefix(executorProps.threadNamePrefix());
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
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
