package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO для запроса на генерацию тест-кейсов.
 *
 * @param requirementsText Текстовое описание требований, фичи или пользовательской истории.
 */
@Schema(description = "DTO для запроса на генерацию тест-кейсов")
public record TestCaseGenerationRequest(
        @Schema(description = "Текстовое описание требований для анализа", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Пользователь должен иметь возможность войти в систему, используя email и пароль. " +
                        "Если учетные данные верны, он перенаправляется на главную страницу. " +
                        "Если нет, отображается сообщение об ошибке.")
        @NotBlank(message = "Текст требований не может быть пустым")
        @Size(min = 20, max = 8192, message = "Длина текста требований должна быть от 20 до 8192 символов")
        String requirementsText
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер агентов.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("requirementsText", requirementsText));
    }
}
