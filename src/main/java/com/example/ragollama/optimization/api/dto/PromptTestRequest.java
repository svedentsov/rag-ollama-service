package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на A/B-тестирование промпта.
 *
 * @param promptName       Имя шаблона промпта в camelCase (например, "ragPrompt").
 * @param newPromptContent Полное новое содержимое шаблона для варианта B.
 */
@Schema(description = "DTO для запроса на A/B-тестирование промпта")
public record PromptTestRequest(
        @Schema(description = "Имя шаблона в camelCase (например, 'ragPrompt')", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String promptName,

        @Schema(description = "Новое содержимое шаблона для тестирования", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Length(min = 50)
        String newPromptContent
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "promptName", this.promptName,
                "newPromptContent", this.newPromptContent
        ));
    }
}