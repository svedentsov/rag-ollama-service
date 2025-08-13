package com.example.ragollama.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO для ответа от чата.
 *
 * @param responseMessage Ответ, сгенерированный AI.
 * @param sessionId       Идентификатор сессии, к которой относится ответ.
 */
@Schema(description = "DTO для ответа от чата")
public record ChatResponse(
        @Schema(description = "Ответ, сгенерированный AI", example = "Все отлично! Чем могу помочь?")
        String responseMessage,
        @Schema(description = "ID сессии для продолжения диалога", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID sessionId
) {
}
