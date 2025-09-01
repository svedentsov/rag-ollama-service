package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на проверку консистентности утверждения.
 *
 * @param claim Текстовое утверждение, которое необходимо проверить
 *              на соответствие данным из различных источников.
 */
@Schema(description = "DTO для запроса на проверку консистентности утверждения")
public record ConsistencyCheckRequest(
        @Schema(description = "Утверждение для проверки", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Таймаут для Ollama должен быть 280 секунд")
        @NotBlank
        @Length(min = 10, max = 1024)
        String claim
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска конвейера.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("claim", this.claim));
    }
}
