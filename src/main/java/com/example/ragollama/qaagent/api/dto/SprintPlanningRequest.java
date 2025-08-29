package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO для запроса на планирование спринта.
 *
 * @param analysisPeriodDays Период для анализа исторических данных о багах.
 */
@Schema(description = "DTO для запроса на планирование спринта")
public record SprintPlanningRequest(
        @Schema(description = "Период для анализа истории багов в днях", defaultValue = "90")
        @NotNull @Min(30) @Max(365)
        Integer analysisPeriodDays
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("analysisPeriodDays", this.analysisPeriodDays));
    }
}
