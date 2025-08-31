package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одной конкретной рекомендации по оптимизации.
 *
 * @param parameterPath  Путь к параметру в `application.yml` (например, "app.rag.retrieval.hybrid.vector-search.top-k").
 * @param currentValue   Текущее значение параметра.
 * @param suggestedValue Предлагаемое новое значение.
 * @param justification  Обоснование от AI, почему это изменение должно улучшить метрики.
 * @param expectedImpact Ожидаемое влияние на метрики (например, "Повышение Recall на 5-10%").
 */
@Schema(description = "Одна рекомендация по оптимизации")
@JsonIgnoreProperties(ignoreUnknown = true)
public record OptimizationRecommendation(
        String parameterPath,
        String currentValue,
        String suggestedValue,
        String justification,
        String expectedImpact
) {
}
