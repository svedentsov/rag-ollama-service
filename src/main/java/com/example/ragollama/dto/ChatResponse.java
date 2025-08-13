package com.example.ragollama.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO (Data Transfer Object) для ответа от чата.
 * <p>
 * Содержит информацию, возвращаемую пользователю после обработки его запроса в чате.
 *
 * @param responseMessage Ответ, сгенерированный языковой моделью (LLM).
 * @param sessionId       Идентификатор сессии, к которой относится данный ответ.
 *                        Клиент должен использовать этот ID для последующих запросов,
 *                        чтобы продолжить диалог в том же контексте.
 */
@Schema(description = "DTO для ответа от чата")
public record ChatResponse(
        @Schema(description = "Ответ, сгенерированный AI", example = "Все отлично! Чем могу помочь?")
        String responseMessage,

        @Schema(description = "ID сессии для продолжения диалога", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID sessionId
) {
}
