package com.example.ragollama.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    /**
     * Создает и настраивает бин {@link WebClient} для HTTP-взаимодействий.
     * Эта конфигурация является production-ready, так как включает все необходимые
     * таймауты для обеспечения отказоустойчивости и предотвращения исчерпания
     * ресурсов при проблемах с внешними сервисами (например, Ollama).
     *
     * @return Сконфигурированный, отказоустойчивый экземпляр {@link WebClient}.
     */
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(120))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS)));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * Создает единый пул потоков для всех асинхронных задач.
     * Возвращает конкретный тип AsyncTaskExecutor, который требуется сервисам.
     *
     * @return Сконфигурированный и управляемый Spring'ом {@link AsyncTaskExecutor}.
     */
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("app-task-exec-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }

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
