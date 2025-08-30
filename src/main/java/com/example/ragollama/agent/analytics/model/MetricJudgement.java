package com.example.ragollama.agent.analytics.model;

import com.example.ragollama.agent.analytics.domain.CanaryAnalysisService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления результата анализа одной метрики.
 *
 * @param metricName        Имя метрики (например, "latency_p99_ms").
 * @param statisticalResult Результат статистического теста.
 * @param pValue            Рассчитанное p-value.
 * @param interpretation    Человекочитаемое объяснение результата от LLM.
 */
@Schema(description = "Результат анализа одной метрики")
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetricJudgement(
        @Schema(description = "Имя метрики")
        String metricName,
        @Schema(description = "Результат статистического теста")
        CanaryAnalysisService.StatisticalResult statisticalResult,
        @Schema(description = "Рассчитанное p-value")
        double pValue,
        @Schema(description = "Объяснение от AI")
        String interpretation
) {
}
