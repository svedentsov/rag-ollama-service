package com.example.ragollama.agent.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от агента-анализатора канареечных развертываний.
 *
 * @param overallDecision  Финальный вердикт ("GO", "NO_GO", "WATCH").
 * @param executiveSummary Краткое резюме для менеджмента, сгенерированное LLM.
 * @param metricJudgements Детальный разбор по каждой проанализированной метрике.
 */
@Schema(description = "Финальный отчет по анализу канареечного развертывания")
@JsonIgnoreProperties(ignoreUnknown = true)
public record CanaryAnalysisReport(
        @Schema(description = "Итоговое решение (GO, NO_GO, WATCH)")
        String overallDecision,
        @Schema(description = "Резюме для руководства")
        String executiveSummary,
        @Schema(description = "Детальный анализ по каждой метрике")
        List<MetricJudgement> metricJudgements
) {
}
