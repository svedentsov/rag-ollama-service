package com.example.ragollama.agent.ux.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на оценку UX по эвристикам.
 *
 * @param htmlContent Полный HTML-код страницы или компонента для анализа.
 */
@Schema(description = "DTO для запроса на оценку UX по эвристикам")
public record UxHeuristicsRequest(
        @Schema(description = "HTML-код страницы для анализа", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Length(min = 50)
        String htmlContent
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("htmlContent", this.htmlContent));
    }
}
