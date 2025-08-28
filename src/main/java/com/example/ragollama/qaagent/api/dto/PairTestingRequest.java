package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на запуск парного тестирования.
 *
 * @param requirementsText Текстовое описание требований или пользовательской истории.
 */
@Schema(description = "DTO для запроса на запуск парного тестирования")
public record PairTestingRequest(
        @Schema(description = "Описание требований для генерации тестов", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Length(min = 20)
        String requirementsText
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("requirementsText", this.requirementsText));
    }
}
