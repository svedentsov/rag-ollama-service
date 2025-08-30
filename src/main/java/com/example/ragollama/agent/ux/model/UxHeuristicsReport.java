package com.example.ragollama.agent.ux.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от UX Heuristics Evaluator.
 *
 * @param overallScore Общая оценка юзабилити по 100-балльной шкале.
 * @param summary      Краткое резюме от AI-эксперта.
 * @param violations   Список обнаруженных нарушений эвристик.
 */
@Schema(description = "Отчет об оценке UX по эвристикам Нильсена")
@JsonIgnoreProperties(ignoreUnknown = true)
public record UxHeuristicsReport(
        @Schema(description = "Общая оценка юзабилити (1-100)")
        int overallScore,
        @Schema(description = "Краткое резюме и ключевые выводы")
        String summary,
        @Schema(description = "Список обнаруженных нарушений эвристик")
        List<HeuristicViolation> violations
) {
}
