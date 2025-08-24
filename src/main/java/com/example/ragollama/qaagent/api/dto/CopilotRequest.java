package com.example.ragollama.qaagent.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO для запроса к QA Copilot.
 *
 * @param message   Сообщение от пользователя.
 * @param sessionId Опциональный ID сессии для продолжения диалога.
 */
@Schema(description = "DTO для запроса в чат с QA Copilot")
public record CopilotRequest(
        @Schema(description = "Сообщение или задача от пользователя", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Найди риски безопасности в изменениях между main и feature/new-auth")
        @NotBlank @Size(max = 2048)
        String message,

        @Schema(description = "Опциональный ID сессии для продолжения диалога")
        UUID sessionId
) {
}
