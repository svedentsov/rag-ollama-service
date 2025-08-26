package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO для высокоуровневого запроса к SDLC Orchestrator.
 *
 * @param goal           Высокоуровневая цель на естественном языке.
 * @param initialContext Карта с начальными данными для выполнения цели.
 */
@Schema(description = "DTO для запроса к SDLC Orchestrator")
public record SdlcRequest(
        @Schema(description = "Высокоуровневая цель на естественном языке", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Проведи полный аудит готовности к релизу для ветки feature/new-payment-system")
        @NotBlank @Size(max = 1024)
        String goal,

        @Schema(description = "Начальный контекст с данными для выполнения",
                example = "{\"oldRef\": \"main\", \"newRef\": \"feature/new-payment-system\"}")
        Map<String, Object> initialContext
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(initialContext != null ? initialContext : Map.of());
    }
}
