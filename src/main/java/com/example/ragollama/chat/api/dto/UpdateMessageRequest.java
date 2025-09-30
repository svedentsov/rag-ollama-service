package com.example.ragollama.chat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO для обновления содержимого сообщения")
public record UpdateMessageRequest(
        @Schema(description = "Новый текст сообщения", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Содержимое не может быть пустым")
        @Size(max = 4096, message = "Сообщение не должно превышать 4096 символов")
        String newContent
) {
}
