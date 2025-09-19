package com.example.ragollama.agent.registry.model;

import com.example.ragollama.agent.ToolAgent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO для описания агента-инструмента при его динамической регистрации.
 *
 * @param name                Уникальное имя агента.
 * @param description         Описание для LLM-планировщика.
 * @param implementationClass Полное имя класса, реализующего {@link ToolAgent}.
 */
@Schema(description = "Описание агента-инструмента для динамической регистрации")
public record AgentDefinition(
        @Schema(description = "Уникальное имя агента", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String name,

        @Schema(description = "Описание для LLM-планировщика", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String description,

        @Schema(description = "Полное имя класса реализации", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "com.example.ragollama.agent.git.domain.GitInspectorAgent")
        @NotBlank
        String implementationClass
) {
}