package com.example.ragollama.shared.config;

import com.example.ragollama.shared.config.properties.AppProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.netty.http.client.HttpClient;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Основной конфигурационный класс приложения.
 * <p>
 * В эту версию добавлена настройка `MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS`,
 * чтобы сделать десериализацию Enum более устойчивой к регистру.
 */
@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final AppProperties appProperties;
    private final DataSourceProperties dataSourceProperties;

    @PostConstruct
    void initializeReactorContext() {
        Hooks.enableAutomaticContextPropagation();
    }

    /**
     * Создает и настраивает основной, строгий бин ObjectMapper.
     *
     * @return Сконфигурированный экземпляр ObjectMapper.
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
     * Создает отдельный, "снисходительный" бин ObjectMapper для парсинга
     * потенциально "грязных" JSON-ответов от LLM.
     *
     * @param primaryObjectMapper Основной, строго настроенный ObjectMapper.
     * @return Новый экземпляр ObjectMapper с включенными флагами для гибкого парсинга.
     */
    @Bean
    @Qualifier("permissiveObjectMapper")
    public ObjectMapper permissiveObjectMapper(ObjectMapper primaryObjectMapper) {
        return primaryObjectMapper.copy()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);
    }

    /**
     * Создает и настраивает глобальный WebClient.Builder с таймаутами.
     *
     * @param objectMapper Сконфигурированный ObjectMapper.
     * @return Преднастроенный WebClient.Builder.
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
     * Явно создает бин {@link DataSource} для JDBC-компонентов.
     *
     * @return Сконфигурированный {@link DataSource}.
     */
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(dataSourceProperties.getUrl())
                .username(dataSourceProperties.getUsername())
                .password(dataSourceProperties.getPassword())
                .driverClassName(dataSourceProperties.getDriverClassName())
                .build();
    }

    /**
     * Создает бин {@link JdbcTemplate}, используя явно созданный DataSource.
     *
     * @param dataSource Бин DataSource, созданный методом выше.
     * @return Готовый к использованию JdbcTemplate.
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Создает основной пул потоков для общих асинхронных задач.
     *
     * @return Сконфигурированный AsyncTaskExecutor.
     */
    @Bean
    @Primary
    public AsyncTaskExecutor applicationTaskExecutor() {
        return createExecutor(appProperties.taskExecutor(), "app-async-");
    }

    /**
     * Создает выделенный пул потоков для долгих вызовов к LLM.
     *
     * @return Сконфигурированный AsyncTaskExecutor.
     */
    @Bean
    public AsyncTaskExecutor llmTaskExecutor() {
        return createExecutor(appProperties.llmExecutor(), "llm-async-");
    }

    /**
     * Создает выделенный пул потоков для блокирующих операций с БД (через JdbcTemplate).
     *
     * @return Сконфигурированный AsyncTaskExecutor.
     */
    @Bean
    public AsyncTaskExecutor databaseTaskExecutor() {
        return createExecutor(appProperties.dbExecutor(), "db-async-");
    }

    private AsyncTaskExecutor createExecutor(AppProperties.TaskExecutor props, String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.corePoolSize());
        executor.setMaxPoolSize(props.maxPoolSize());
        executor.setQueueCapacity(props.queueCapacity());
        executor.setThreadNamePrefix(prefix);
        executor.setTaskDecorator(new ContextAwareTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * Декоратор задач, обеспечивающий проброс контекста MDC в асинхронные потоки.
     */
    static class ContextAwareTaskDecorator implements TaskDecorator {
        @Override
        @NonNull
        public Runnable decorate(@NonNull Runnable runnable) {
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
     *
     * @return ScheduledExecutorService.
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
