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
 * <p> Этот record-класс использует возможности {@code @ConfigurationProperties} для
 * обеспечения типобезопасного, иммутабельного и самодокументируемого доступа
 * к конфигурации. Каждый вложенный record соответствует логическому блоку в {@code application.yml}.
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
     *
     * @param ragTemplatePath Путь к основному шаблону для RAG-запросов.
     */
    public record Prompt(@NotBlank String ragTemplatePath) {
    }

    /**
     * Настройки для сервиса переранжирования.
     *
     * @param enabled           Включает/выключает сервис.
     * @param keywordMatchBoost Фактор усиления для совпадений по ключевым словам.
     */
    public record Reranking(boolean enabled, double keywordMatchBoost) {
    }

    /**
     * Настройки токенизатора (библиотека jtokkit).
     *
     * @param encodingModel Имя модели кодирования (например, "o200k_base").
     */
    public record Tokenization(@NotBlank String encodingModel) {
    }

    /**
     * Настройки для сборки контекста RAG.
     *
     * @param maxTokens Максимальное количество токенов, разрешенное для контекста.
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
         * @param maxMessages Количество последних сообщений для поддержания контекста.
         */
        public record History(@Min(1) @Max(50) int maxMessages) {
        }
    }

    /**
     * Настройки для HTTP-клиентов, таких как WebClient.
     *
     * @param connectTimeout   Таймаут на установку TCP-соединения.
     * @param responseTimeout  Общий таймаут на получение ответа.
     * @param readWriteTimeout Таймаут на чтение/запись данных в установленном соединении.
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
     *
     * @param corePoolSize     Базовый размер пула.
     * @param maxPoolSize      Максимальный размер пула.
     * @param queueCapacity    Размер очереди для задач.
     * @param threadNamePrefix Префикс для имен потоков.
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
     *
     * @param index Параметры HNSW-индекса.
     */
    @Validated
    public record VectorStoreProperties(@NotNull Index index) {
        /**
         * @param m              Количество соседей на слой графа HNSW.
         * @param efConstruction Глубина поиска при построении индекса.
         * @param efSearch       Глубина поиска во время запроса.
         */
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
     *
     * @param graph Настройки для расширения через граф знаний.
     */
    @Validated
    public record Expansion(@NotNull Graph graph) {
        /**
         * @param enabled Включает/выключает шаг расширения через граф.
         */
        public record Graph(boolean enabled) {
        }
    }

    /**
     * Общие настройки для всего RAG-конвейера.
     *
     * @param noContextStrategy   Стратегия поведения при отсутствии контекста.
     * @param arrangementStrategy Стратегия компоновки финального контекста.
     * @param summarizer          Настройки суммаризации.
     * @param retrieval           Настройки извлечения.
     * @param validation          Настройки валидации ответа.
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
     *
     * @param enabled Включает/выключает.
     */
    public record Summarizer(boolean enabled) {
    }

    /**
     * Настройки для шага валидации ответа.
     *
     * @param enabled Включает/выключает AI-критика.
     */
    public record Validation(boolean enabled) {
    }
}
