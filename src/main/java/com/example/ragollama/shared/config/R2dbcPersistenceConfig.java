package com.example.ragollama.shared.config;

import com.example.ragollama.rag.domain.model.QueryFormationStep;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.ConnectionFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Централизованная и единственная конфигурация для персистентности R2DBC.
 * <p>
 * Эта конфигурация явно указывает Spring, где искать R2DBC-репозитории,
 * что предотвращает конфликты с другими модулями Spring Data (например, Neo4j).
 * Также регистрирует кастомные конвертеры для корректной работы с типами данных,
 * специфичными для PostgreSQL (JSONB) и Java (OffsetDateTime, Map).
 */
@Slf4j
@Configuration
@EnableR2dbcRepositories(basePackages = {
        "com.example.ragollama.chat.domain",
        "com.example.ragollama.ingestion.domain",
        "com.example.ragollama.monitoring.domain",
        "com.example.ragollama.agent.metrics.domain",
        "com.example.ragollama.agent.dynamic",
        "com.example.ragollama.agent.finops.domain",
        "com.example.ragollama.evaluation.domain",
        "com.example.ragollama.shared.task",
        "com.example.ragollama.web"})
@EnableR2dbcAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
@EnableTransactionManagement
@RequiredArgsConstructor
public class R2dbcPersistenceConfig {

    private final ObjectMapper objectMapper;

    /**
     * Предоставляет основной менеджер транзакций для реактивного стека.
     *
     * @param connectionFactory Фабрика соединений R2DBC.
     * @return Сконфигурированный менеджер транзакций.
     */
    @Bean
    @Primary
    public ReactiveTransactionManager r2dbcTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    /**
     * Провайдер для автоматического проставления временных меток в полях,
     * аннотированных {@code @CreatedDate} и {@code @LastModifiedDate}.
     *
     * @return Провайдер, возвращающий текущее время как {@link OffsetDateTime}.
     */
    @Bean(name = "offsetDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }

    /**
     * Регистрирует все кастомные конвертеры типов для R2DBC.
     *
     * @return Бин {@link R2dbcCustomConversions} с набором конвертеров.
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = List.of(
                new MapToJsonConverter(objectMapper),
                new JsonToMapConverter(objectMapper),
                new ListSourceCitationToJsonConverter(objectMapper),
                new JsonToListSourceCitationConverter(objectMapper),
                new ListQueryFormationStepToJsonConverter(objectMapper),
                new JsonToListQueryFormationStepConverter(objectMapper),
                new OffsetDateTimeReadConverter(),
                new OffsetDateTimeWriteConverter()
        );
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }

    // --- Converters for OffsetDateTime ---
    @ReadingConverter
    private static class OffsetDateTimeReadConverter implements Converter<LocalDateTime, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(@NonNull LocalDateTime source) {
            return source.atOffset(ZoneOffset.UTC);
        }
    }

    @WritingConverter
    private static class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, LocalDateTime> {
        @Override
        public LocalDateTime convert(@NonNull OffsetDateTime source) {
            return source.toLocalDateTime();
        }
    }

    // --- Converters for Map<String, Object> <-> JSONB ---
    @WritingConverter
    @RequiredArgsConstructor
    private static class MapToJsonConverter implements Converter<Map<String, Object>, Json> {
        private final ObjectMapper objectMapper;

        @Override
        public Json convert(@NonNull Map<String, Object> source) {
            try {
                return source.isEmpty() ? Json.of("{}") : Json.of(objectMapper.writeValueAsBytes(source));
            } catch (JsonProcessingException e) {
                log.error("Не удалось сериализовать Map в JSONB", e);
                throw new IllegalStateException("Ошибка сериализации в JSONB", e);
            }
        }
    }

    @ReadingConverter
    @RequiredArgsConstructor
    private static class JsonToMapConverter implements Converter<Json, Map<String, Object>> {
        private final ObjectMapper objectMapper;

        @Override
        public Map<String, Object> convert(@NonNull Json source) {
            try {
                String jsonString = source.asString();
                if (jsonString == null || jsonString.isBlank() || "null".equalsIgnoreCase(jsonString.trim())) {
                    return Collections.emptyMap();
                }
                return objectMapper.readValue(jsonString, new TypeReference<>() {
                });
            } catch (IOException e) {
                log.error("Не удалось десериализовать JSONB в Map", e);
                return Collections.emptyMap();
            }
        }
    }

    // --- Converters for List<SourceCitation> <-> JSONB ---
    @WritingConverter
    @RequiredArgsConstructor
    private static class ListSourceCitationToJsonConverter implements Converter<List<SourceCitation>, Json> {
        private final ObjectMapper objectMapper;

        @Override
        public Json convert(@NonNull List<SourceCitation> source) {
            try {
                return Json.of(objectMapper.writeValueAsBytes(source));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Ошибка сериализации List<SourceCitation> в JSONB", e);
            }
        }
    }

    @ReadingConverter
    @RequiredArgsConstructor
    private static class JsonToListSourceCitationConverter implements Converter<Json, List<SourceCitation>> {
        private final ObjectMapper objectMapper;

        @Override
        public List<SourceCitation> convert(@NonNull Json source) {
            try {
                return objectMapper.readValue(source.asString(), new TypeReference<>() {
                });
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }
    }

    @WritingConverter
    @RequiredArgsConstructor
    private static class ListQueryFormationStepToJsonConverter implements Converter<List<QueryFormationStep>, Json> {
        private final ObjectMapper objectMapper;

        @Override
        public Json convert(@NonNull List<QueryFormationStep> source) {
            try {
                return Json.of(objectMapper.writeValueAsBytes(source));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Ошибка сериализации List<QueryFormationStep> в JSONB", e);
            }
        }
    }

    @ReadingConverter
    @RequiredArgsConstructor
    private static class JsonToListQueryFormationStepConverter implements Converter<Json, List<QueryFormationStep>> {
        private final ObjectMapper objectMapper;

        @Override
        public List<QueryFormationStep> convert(@NonNull Json source) {
            try {
                return objectMapper.readValue(source.asString(), new TypeReference<>() {
                });
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }
    }
}
