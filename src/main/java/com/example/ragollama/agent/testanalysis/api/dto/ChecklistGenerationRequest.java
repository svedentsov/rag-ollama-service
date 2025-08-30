package com.example.ragollama.agent.testanalysis.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO для запроса на генерацию чек-листа для ручного тестирования.
 *
 * @param featureDescription Текстовое описание требований, фичи или пользовательской истории.
 */
@Schema(description = "DTO для запроса на генерацию чек-листа")
public record ChecklistGenerationRequest(
        @Schema(description = "Текстовое описание требований для анализа", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Пользователь должен иметь возможность сбросить свой пароль. Для этого он вводит свой email, получает на него ссылку, переходит по ней и вводит новый пароль дважды.")
        @NotBlank(message = "Описание функциональности не может быть пустым")
        @Size(min = 20, max = 8192, message = "Длина описания должна быть от 20 до 8192 символов")
        String featureDescription
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер агентов.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("featureDescription", featureDescription));
    }
}
