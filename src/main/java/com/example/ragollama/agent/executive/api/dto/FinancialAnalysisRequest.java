package com.example.ragollama.agent.executive.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO для запроса на финансовый анализ и расчет ROI.
 *
 * @param analysisPeriodDays Период для анализа метрик в днях.
 */
@Schema(description = "DTO для запроса на финансовый анализ")
public record FinancialAnalysisRequest(
        @Schema(description = "Период для анализа метрик в днях", defaultValue = "90")
        @NotNull @Min(30) @Max(365)
        Integer analysisPeriodDays
) {
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("analysisPeriodDays", this.analysisPeriodDays));
    }
}
