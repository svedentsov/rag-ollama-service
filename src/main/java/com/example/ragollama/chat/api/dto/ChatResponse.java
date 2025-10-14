package com.example.ragollama.chat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO для ответа от чата.
 * <p> Содержит информацию, возвращаемую пользователю после обработки его запроса в чате.
 * Является публичным контрактом API.
 *
 * @param responseMessage Ответ, сгенерированный языковой моделью (LLM).
 * @param sessionId       Идентификатор сессии, к которой относится данный ответ.
 * @param finalPrompt     Полный текст промпта, отправленного в LLM.
 */
@Schema(description = "DTO для ответа от чата")
public record ChatResponse(
        @Schema(description = "Ответ, сгенерированный AI", example = "Все отлично! Чем могу помочь?")
        String responseMessage,
        @Schema(description = "ID сессии для продолжения диалога", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID sessionId,
        @Schema(description = "Финальный промпт, отправленный в LLM")
        String finalPrompt
) {
}
