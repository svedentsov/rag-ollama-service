package com.example.ragollama.chat.api.dto;

import com.example.ragollama.orchestration.dto.UniversalRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "DTO для запроса в чат")
public record ChatRequest(
        @Schema(description = "Сообщение от пользователя", requiredMode = Schema.RequiredMode.REQUIRED, example = "Привет, как дела?")
        @NotBlank(message = "Сообщение не может быть пустым")
        @Size(max = 4096, message = "Сообщение не должно превышать 4096 символов")
        String message,

        @Schema(description = "Опциональный ID сессии для продолжения диалога", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID sessionId
) {
    /**
     * Преобразует ChatRequest в универсальный UniversalRequest.
     *
     * @return Экземпляр UniversalRequest.
     */
    public UniversalRequest toUniversalRequest() {
        return new UniversalRequest(this.message, this.sessionId, null, null, null);
    }
}
