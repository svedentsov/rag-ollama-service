package com.example.ragollama.agent.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального, структурированного отчета о готовности релиза.
 *
 * @param readinessDecision Финальный вердикт ("GO", "GO_WITH_CAUTION", "NO_GO").
 * @param confidenceScore   Уверенность LLM в вердикте (0-100).
 * @param executiveSummary  Краткое резюме для менеджмента.
 * @param riskFactors       Детальный список обнаруженных рисков.
 * @param recommendations   Список действенных рекомендаций.
 */
@Schema(description = "Финальный отчет об оценке готовности релиза")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseReadinessReport(
        String readinessDecision,
        int confidenceScore,
        String executiveSummary,
        List<RiskFactor> riskFactors,
        List<String> recommendations
) {
}
