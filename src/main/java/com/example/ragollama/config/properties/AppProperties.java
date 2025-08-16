package com.example.ragollama.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Главный класс для всех кастомных настроек приложения, загружаемых из application.yml с префиксом "app".
 *
 * @param prompt          Настройки, связанные с шаблонами промптов.
 * @param reranking       Настройки для опционального сервиса переранжирования.
 * @param tokenization    Настройки токенизатора.
 * @param context         Настройки сборки контекста для RAG.
 * @param chat            Настройки для функционала чата.
 * @param ingestion       Настройки для процесса фоновой индексации документов.
 * @param httpClient      Настройки для HTTP-клиентов, таких как WebClient.
 * @param taskExecutor    Настройки для основного пула асинхронных задач.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotNull Prompt prompt,
        @NotNull Reranking reranking,
        @NotNull Tokenization tokenization,
        @NotNull Context context,
        @NotNull Chat chat,
        @NotNull Ingestion ingestion,
        @NotNull HttpClient httpClient,
        @NotNull TaskExecutor taskExecutor
) {
    /** Настройки, связанные с шаблонами промптов. */
    public record Prompt(@NotBlank String ragTemplatePath) {}
    /** Настройки для сервиса переранжирования. */
    public record Reranking(boolean enabled, double keywordMatchBoost) {}
    /** Настройки токенизатора (библиотека jtokkit). */
    public record Tokenization(@NotBlank String encodingModel) {}
    /** Настройки для сборки контекста RAG. */
    public record Context(@Min(512) @Max(16384) int maxTokens) {}
    /** Настройки для функционала чата. */
    public record Chat(@NotNull History history) {
        /** Настройки истории чата. */
        public record History(@Min(1) @Max(50) int maxMessages) {}
    }
    /** Настройки для процесса индексации документов. */
    public record Ingestion(@NotNull Scheduler scheduler) {
        /** Настройки планировщика индексации. */
        public record Scheduler(@Min(1000) long delayMs) {}
    }

    /**
     * Настройки для HTTP-клиентов, таких как WebClient.
     * Позволяет централизованно управлять таймаутами для внешних вызовов.
     *
     * @param connectTimeout Таймаут на установку соединения.
     * @param responseTimeout Общий таймаут на получение всего ответа.
     * @param readWriteTimeout Таймаут на чтение/запись данных в рамках установленного соединения.
     */
    @Validated
    public record HttpClient(
            @NotNull Duration connectTimeout,
            @NotNull Duration responseTimeout,
            @NotNull Duration readWriteTimeout
    ) {}

    /**
     * Настройки для основного пула асинхронных задач приложения.
     * Позволяет гибко настраивать параллелизм и производительность.
     *
     * @param corePoolSize Базовое количество потоков в пуле.
     * @param maxPoolSize  Максимальное количество потоков, до которого пул может расшириться.
     * @param queueCapacity Размер очереди для задач, ожидающих выполнения.
     * @param threadNamePrefix Префикс для имен потоков, полезно для отладки и профилирования.
     */
    @Validated
    public record TaskExecutor(
            @Min(1) int corePoolSize,
            @Min(1) int maxPoolSize,
            @Min(0) int queueCapacity,
            @NotBlank String threadNamePrefix
    ) {}
}
