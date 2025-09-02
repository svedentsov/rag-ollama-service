package com.example.ragollama.agent.executive.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO для запроса на анализ инженерной производительности.
 *
 * @param analysisPeriodDays Период для анализа метрик в днях.
 */
@Schema(description = "DTO для запроса на анализ инженерной производительности")
public record EngineeringVelocityRequest(
        @Schema(description = "Период для анализа метрик в днях", defaultValue = "30")
        @NotNull @Min(7) @Max(180)
        Integer analysisPeriodDays
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("analysisPeriodDays", this.analysisPeriodDays));
    }
}
