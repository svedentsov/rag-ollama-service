package com.example.ragollama.chat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO для запроса в чат.
 *
 * @param message   Сообщение от пользователя. Не может быть пустым и ограничено по длине.
 * @param sessionId Опциональный идентификатор сессии. Если предоставлен, диалог
 *                  продолжается в рамках существующей сессии. Если нет, создается новая.
 */
@Schema(description = "DTO для запроса в чат")
public record ChatRequest(
        @Schema(description = "Сообщение от пользователя", requiredMode = Schema.RequiredMode.REQUIRED, example = "Привет, как дела?")
        @NotBlank(message = "Сообщение не может быть пустым")
        @Size(max = 4096, message = "Сообщение не должно превышать 4096 символов")
        String message,

        @Schema(description = "Опциональный ID сессии для продолжения диалога", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID sessionId
) {
}
