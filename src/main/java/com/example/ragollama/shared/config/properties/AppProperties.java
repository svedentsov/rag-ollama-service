package com.example.ragollama.shared.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Главный класс для кастомных настроек приложения, загружаемых из application.yml с префиксом "app".
 * Этот record-класс является "корневым" для общей конфигурации приложения, в то время как
 * более специфичные модули (retrieval, ingestion, reranking) имеют свои собственные
 * классы @ConfigurationProperties для лучшей инкапсуляции.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotNull Prompt prompt,
        @NotNull Tokenization tokenization,
        @NotNull Context context,
        @NotNull Chat chat,
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
     *
     * @param ragTemplatePath Путь к основному шаблону для RAG-запросов.
     */
    public record Prompt(@NotBlank String ragTemplatePath) {
    }

    /**
     * Настройки токенизатора.
     *
     * @param encodingModel Имя модели кодирования (например, "o200k_base").
     */
    public record Tokenization(@NotBlank String encodingModel) {
    }

    /**
     * Настройки для сборки контекста RAG.
     *
     * @param maxTokens Максимальное количество токенов для контекста.
     */
    public record Context(@Min(512) @Max(16384) int maxTokens) {
    }

    /**
     * Настройки для функционала чата.
     *
     * @param history Конфигурация истории чата.
     */
    public record Chat(@NotNull History history) {
        /**
         * @param maxMessages Количество последних сообщений для контекста.
         */
        public record History(@Min(1) @Max(50) int maxMessages) {
        }
    }

    /**
     * Настройки для HTTP-клиентов.
     */
    @Validated
    public record HttpClient(
            @NotNull Duration connectTimeout,
            @NotNull Duration responseTimeout,
            @NotNull Duration readWriteTimeout,
            @NotNull Ollama ollama
    ) {
        @Validated
        public record Ollama(
                @NotNull Duration responseTimeout,
                @NotNull Duration readWriteTimeout
        ) {
        }
    }

    /**
     * Настройки для пула асинхронных задач.
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
     * Общие настройки для RAG-конвейера.
     * Специфичные настройки (retrieval, reranking) вынесены в отдельные классы.
     */
    @Validated
    public record Rag(
            @NotBlank String noContextStrategy,
            @NotBlank String arrangementStrategy,
            @NotNull Summarizer summarizer,
            @NotNull Validation validation
    ) {
    }

    public record Summarizer(boolean enabled) {
    }

    public record Validation(boolean enabled) {
    }
}
