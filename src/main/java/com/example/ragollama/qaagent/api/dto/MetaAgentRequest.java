package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO для запроса к мета-агентам, которые анализируют исторические данные.
 *
 * @param analysisPeriodDays Период для анализа в днях.
 */
@Schema(description = "DTO для запроса к стратегическим мета-агентам")
public record MetaAgentRequest(
        @Schema(description = "Период для анализа в днях", defaultValue = "90")
        @NotNull @Min(7) @Max(365)
        Integer analysisPeriodDays
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("analysisPeriodDays", this.analysisPeriodDays, "days", this.analysisPeriodDays));
    }
}
