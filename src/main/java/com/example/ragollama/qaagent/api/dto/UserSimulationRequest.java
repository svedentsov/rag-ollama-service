package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO для запроса на симуляцию поведения пользователя.
 *
 * @param startUrl Начальный URL для симуляции.
 * @param goal     Высокоуровневая цель на естественном языке.
 */
@Schema(description = "DTO для запроса на симуляцию поведения пользователя")
public record UserSimulationRequest(
        @Schema(description = "Начальный URL для симуляции", requiredMode = Schema.RequiredMode.REQUIRED, example = "http://localhost:3000/login")
        @NotBlank
        String startUrl,
        @Schema(description = "Высокоуровневая цель на естественном языке", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Войти в систему с логином 'user' и паролем 'password', затем перейти в профиль и изменить имя на 'John Doe'.")
        @NotBlank @Size(min = 20)
        String goal
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "startUrl", this.startUrl,
                "goal", this.goal
        ));
    }
}
