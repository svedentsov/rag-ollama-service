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
        @Schema(description = "Резюме для руководства")
        String executiveSummary,
        @Schema(description = "Разбивка затрат по категориям")
        CostBreakdown costBreakdown,
        @Schema(description = "Анализ отдельных инженерных инициатив")
        List<FinancialInitiative> initiativeAnalysis
) {
    /**
     * DTO для разбивки затрат.
     *
     * @param totalCost       Общая сумма затрат.
     * @param costsByCategory Затраты, сгруппированные по категориям.
     */
    @Schema(description = "Разбивка затрат по категориям")
    public record CostBreakdown(double totalCost, Map<String, Double> costsByCategory) {
    }

    /**
     * DTO для анализа одной инициативы.
     *
     * @param name           Название инициативы (например, фича или эпик).
     * @param totalCost      Рассчитанная стоимость реализации.
     * @param valueMetric    Ключевая метрика, отражающая ценность (например, MAU).
     * @param estimatedRoi   Качественная оценка ROI (High, Medium, Low).
     * @param recommendation Стратегическая рекомендация по инициативе.
     */
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
