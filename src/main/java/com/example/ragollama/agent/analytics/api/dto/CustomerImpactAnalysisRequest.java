package com.example.ragollama.agent.analytics.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO для запроса на анализ влияния на конечных пользователей.
 *
 * @param oldRef Исходная Git-ссылка.
 * @param newRef Конечная Git-ссылка.
 */
@Schema(description = "DTO для запроса на анализ влияния на пользователей")
public record CustomerImpactAnalysisRequest(
        @Schema(description = "Исходная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/new-logic")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "oldRef", oldRef,
                "newRef", newRef
        ));
    }
}
