package com.example.ragollama.rag.retrieval;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Типобезопасная конфигурация для стратегий извлечения (Retrieval).
 * <p>
 * Позволяет гибко настраивать параметры гибридного поиска через {@code application.yml}
 * без необходимости изменять код. Включает параметр для управления адаптивной стратегией.
 *
 * @param hybrid Настройки для гибридной стратегии поиска.
 */
@Validated
@ConfigurationProperties(prefix = "app.rag.retrieval")
public record RetrievalProperties(Hybrid hybrid) {
    /**
     * Настройки для гибридной стратегии, сочетающей векторный и полнотекстовый поиск.
     *
     * @param vectorSearch              Настройки для векторного (семантического) поиска.
     * @param fts                       Настройки для полнотекстового (FTS, лексического) поиска.
     * @param expansionMinDocsThreshold Порог для адаптивной стратегии. Если после первого (точного)
     *                                  поиска найдено меньше этого числа документов, запускается
     *                                  вторая волна поиска по расширенным запросам.
     */
    public record Hybrid(
            VectorSearch vectorSearch,
            Fts fts,
            @Min(1) @Max(10) int expansionMinDocsThreshold
    ) {
        /**
         * @param topK                Количество наиболее релевантных документов для извлечения.
         * @param similarityThreshold Минимальный порог схожести (0.0 до 1.0).
         */
        public record VectorSearch(
                @Min(1) @Max(20) int topK,
                @DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold
        ) {
        }

        /**
         * @param topK Количество наиболее релевантных документов для извлечения.
         */
        public record Fts(@Min(1) @Max(20) int topK) {
        }
    }
}
