package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "DTO для запуска агента самосовершенствования")
public record SelfImprovementRequest(
        @Schema(description = "Период для анализа логов выполнения в днях", defaultValue = "7")
        @NotNull @Min(1) @Max(90)
        Integer analysisPeriodDays
) {
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("analysisPeriodDays", this.analysisPeriodDays));
    }
}
