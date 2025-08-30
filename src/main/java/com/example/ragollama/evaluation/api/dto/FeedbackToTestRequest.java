package com.example.ragollama.evaluation.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * DTO для запроса на преобразование фидбэка в тест.
 *
 * @param feedbackId ID записи в `feedback_log`.
 */
@Schema(description = "DTO для запроса на создание теста из фидбэка")
public record FeedbackToTestRequest(
        @Schema(description = "ID записи об обратной связи", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UUID feedbackId
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("feedbackId", this.feedbackId));
    }
}
