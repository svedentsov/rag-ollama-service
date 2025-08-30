package com.example.ragollama.agent.mlops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от ML/Feature Drift Guard.
 *
 * @param overallVerdict   Вердикт AI-аналитика ("OK", "DRIFT_DETECTED", "CRITICAL_DRIFT").
 * @param executiveSummary Резюме для менеджмента.
 * @param detailedAnalysis Детальный анализ по каждому признаку с дрейфом.
 */
@Schema(description = "Финальный отчет об анализе дрейфа признаков")
@JsonIgnoreProperties(ignoreUnknown = true)
public record DriftReport(
        String overallVerdict,
        String executiveSummary,
        List<DriftAnalysisResult> detailedAnalysis
) {
}
