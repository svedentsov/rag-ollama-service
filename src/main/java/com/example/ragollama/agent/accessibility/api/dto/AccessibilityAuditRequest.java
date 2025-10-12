package com.example.ragollama.agent.accessibility.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на аудит доступности (a11y).
 * <p>
 * Этот record является частью публичного API и валидируется на входе в контроллер.
 *
 * @param htmlContent Полный HTML-код страницы для анализа.
 */
@Schema(description = "DTO для запроса на аудит доступности (a11y)")
public record AccessibilityAuditRequest(
        @Schema(description = "Полный HTML-код страницы для анализа", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "<html><body><img src='logo.png'></body></html>")
        @NotBlank(message = "HTML-контент не может быть пустым")
        @Length(min = 20, message = "HTML-контент слишком короткий для полноценного анализа")
        String htmlContent
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер агентов.
     * <p>
     * Этот метод является частью "антикоррупционного слоя", изолируя
     * внутреннюю структуру {@link AgentContext} от публичного API.
     *
     * @return Контекст для запуска агента, содержащий HTML-контент.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("htmlContent", this.htmlContent));
    }
}
