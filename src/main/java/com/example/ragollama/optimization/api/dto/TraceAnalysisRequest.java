package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO для запроса на анализ распределенного трейса.
 *
 * @param requestId Уникальный идентификатор запроса (X-Request-ID),
 *                  трейс которого необходимо проанализировать.
 */
@Schema(description = "DTO для запроса на анализ распределенного трейса")
public record TraceAnalysisRequest(
        @Schema(description = "ID запроса (X-Request-ID) для анализа", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String requestId
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("requestId", this.requestId));
    }
}