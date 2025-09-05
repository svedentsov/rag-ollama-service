package com.example.ragollama.agent.performance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для полного отчета о прогнозируемом влиянии на производительность.
 *
 * @param predictions Список прогнозов для каждого файла с высоким риском.
 */
@Schema(description = "Отчет о прогнозируемом влиянии на производительность")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerformanceImpactReport(
        List<FilePerformanceImpact> predictions
) {
    /**
     * DTO для прогноза по одному файлу.
     *
     * @param filePath                 Путь к файлу.
     * @param riskLevel                Уровень риска ("High", "Medium", "Low").
     * @param predictedLatencyIncrease Прогнозируемое увеличение p99 задержки в миллисекундах.
     * @param justification            Обоснование прогноза от AI.
     */
    @Schema(description = "Прогноз влияния на производительность для одного файла")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FilePerformanceImpact(
            String filePath,
            String riskLevel,
            int predictedLatencyIncrease,
            String justification
    ) {
    }
}
