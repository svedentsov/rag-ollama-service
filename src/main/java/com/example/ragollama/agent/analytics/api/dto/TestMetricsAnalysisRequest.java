package com.example.ragollama.agent.analytics.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO для запроса на анализ исторических метрик.
 *
 * @param days Период для анализа в днях (от 1 до 90).
 */
@Schema(description = "DTO для запроса на анализ метрик тестирования")
public record TestMetricsAnalysisRequest(
        @Schema(description = "Количество последних дней для анализа", requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "30")
        @Min(1) @Max(90) @NotNull
        Integer days
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("days", days));
    }
}
