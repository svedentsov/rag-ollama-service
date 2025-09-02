package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO для запроса на запуск агента самосовершенствования.
 *
 * @param analysisPeriodDays Период для анализа логов выполнения в днях.
 */
@Schema(description = "DTO для запуска агента самосовершенствования")
public record SelfImprovementRequest(
        @Schema(description = "Период для анализа логов выполнения в днях", defaultValue = "7")
        @NotNull @Min(1) @Max(90)
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
