package com.example.ragollama.optimization.model;

import com.example.ragollama.evaluation.model.EvaluationResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO для финального, структурированного отчета от ExperimentAnalysisAgent.
 * <p>
 * Этот объект агрегирует результаты A/B-тестирования различных конфигураций
 * и представляет их в удобном для анализа виде, включая вердикт от AI-аналитика.
 */
@Schema(description = "Финальный отчет о результатах эксперимента")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExperimentReport(
        @Schema(description = "Имя варианта-победителя (или 'baseline', если никто не лучше)")
        String winningVariant,

        @Schema(description = "Резюме для принятия решений от AI-аналитика")
        String executiveSummary,

        @Schema(description = "Карта с полными результатами оценки для каждого варианта, включая baseline")
        Map<String, EvaluationResult> results
) {
}
