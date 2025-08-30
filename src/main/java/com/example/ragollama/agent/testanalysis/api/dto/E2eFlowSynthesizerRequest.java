package com.example.ragollama.agent.testanalysis.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на синтез E2E-теста.
 *
 * @param userStory Высокоуровневое описание пользовательского сценария на естественном языке.
 */
@Schema(description = "DTO для запроса на синтез E2E-теста")
public record E2eFlowSynthesizerRequest(
        @Schema(description = "Описание пользовательского сценария", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Пользователь регистрируется, входит в систему, добавляет товар в корзину и оформляет заказ.")
        @NotBlank @Length(min = 20)
        String userStory
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("userStory", this.userStory));
    }
}
