package com.example.ragollama.agent.executive.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * DTO для финального отчета от "Financial & ROI Governor".
 */
@Schema(description = "Отчет об анализе затрат и ROI")
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinancialRoiReport(
        String executiveSummary,
        CostBreakdown costBreakdown,
        List<FinancialInitiative> initiativeAnalysis
) {
    @Schema(description = "Разбивка затрат по категориям")
    public record CostBreakdown(double totalCost, Map<String, Double> costsByCategory) {
    }

    @Schema(description = "Анализ одной инженерной инициативы (фичи/эпика)")
    public record FinancialInitiative(
            String name,
            double totalCost,
            String valueMetric,
            String estimatedRoi,
            String recommendation
    ) {
    }
}
