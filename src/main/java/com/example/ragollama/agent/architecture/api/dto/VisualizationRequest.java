package com.example.ragollama.agent.architecture.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO для запроса на генерацию диаграммы зависимостей.
 *
 * @param oldRef Исходная Git-ссылка для анализа.
 * @param newRef Конечная Git-ссылка для анализа.
 */
@Schema(description = "DTO для запроса на генерацию диаграммы зависимостей")
public record VisualizationRequest(
        @Schema(description = "Исходная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/new-module")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска конвейера.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "oldRef", this.oldRef,
                "newRef", this.newRef
        ));
    }
}
