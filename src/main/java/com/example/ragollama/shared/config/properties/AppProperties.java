package com.example.ragollama.shared.config.properties;

import com.example.ragollama.ingestion.IngestionProperties;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Главный класс для всех кастомных настроек приложения, загружаемых из application.yml с префиксом "app".
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotNull Prompt prompt,
        @NotNull Reranking reranking,
        @NotNull Tokenization tokenization,
        @NotNull Context context,
        @NotNull Chat chat,
        @NotNull IngestionProperties ingestion,
        @NotNull HttpClient httpClient,
        @NotNull TaskExecutor taskExecutor,
        @NotNull TaskExecutor llmExecutor,
        @NotNull TaskExecutor dbExecutor,
        @NotNull VectorStoreProperties vectorStore,
        @NotNull Expansion expansion,
        @NotNull Rag rag
) {
    /**
     * Настройки, связанные с шаблонами промптов.
     */
    public record Prompt(@NotBlank String ragTemplatePath) {
    }

    /**
     * Настройки для сервиса переранжирования.
     */
    public record Reranking(boolean enabled, double keywordMatchBoost) {
    }

    /**
     * Настройки токенизатора (библиотека jtokkit).
     */
    public record Tokenization(@NotBlank String encodingModel) {
    }

    /**
     * Настройки для сборки контекста RAG.
     */
    public record Context(@Min(512) @Max(16384) int maxTokens) {
    }

    /**
     * Настройки для функционала чата.
     */
    public record Chat(@NotNull History history) {
        public record History(@Min(1) @Max(50) int maxMessages) {
        }
    }

    /**
     * Настройки для HTTP-клиентов, таких как WebClient.
     */
    @Validated
    public record HttpClient(
            @NotNull Duration connectTimeout,
            @NotNull Duration responseTimeout,
            @NotNull Duration readWriteTimeout
    ) {
    }

    /**
     * Настройки для пула асинхронных задач приложения.
     */
    @Validated
    public record TaskExecutor(
            @Min(1) int corePoolSize,
            @Min(1) int maxPoolSize,
            @Min(0) int queueCapacity,
            @NotBlank String threadNamePrefix
    ) {
    }

    /**
     * Настройки, специфичные для векторного хранилища.
     */
    @Validated
    public record VectorStoreProperties(@NotNull Index index) {
        @Validated
        public record Index(
                @Min(4) @Max(128) int m,
                @Min(8) @Max(1024) int efConstruction,
                @Min(4) @Max(1024) int efSearch
        ) {
        }
    }

    /**
     * Конфигурация для стратегий расширения контекста.
     */
    @Validated
    public record Expansion(@NotNull Graph graph) {
        public record Graph(boolean enabled) {
        }
    }

    /**
     * Общие настройки для всего RAG-конвейера.
     */
    @Validated
    public record Rag(
            @NotBlank String noContextStrategy,
            @NotBlank String arrangementStrategy,
            @NotNull Summarizer summarizer,
            @NotNull RetrievalProperties retrieval,
            @NotNull Validation validation
    ) {
    }

    /**
     * Настройки для суммаризации (в данный момент не используются).
     */
    public record Summarizer(boolean enabled) {
    }

    /**
     * Настройки для шага валидации ответа.
     */
    public record Validation(boolean enabled) {
    }
}
