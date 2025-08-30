package com.example.ragollama.agent.copilot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO для ответа от QA Copilot.
 *
 * @param responseMessage Ответ ассистента в формате Markdown.
 * @param sessionId       ID сессии для продолжения диалога.
 */
@Schema(description = "DTO для ответа от QA Copilot")
public record CopilotResponse(
        @Schema(description = "Ответ ассистента в формате Markdown")
        String responseMessage,

        @Schema(description = "ID сессии для продолжения диалога")
        UUID sessionId
) {
}
