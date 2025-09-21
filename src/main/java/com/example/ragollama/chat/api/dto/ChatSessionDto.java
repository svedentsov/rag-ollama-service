package com.example.ragollama.chat.api.dto;

import com.example.ragollama.chat.domain.model.ChatSession;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatSessionDto(
        UUID sessionId,
        String chatName
) {
    public static ChatSessionDto fromEntity(ChatSession entity) {
        return new ChatSessionDto(entity.getSessionId(), entity.getChatName());
    }

    @Schema(description = "DTO для обновления имени чата")
    public record UpdateRequest(
            @NotBlank @Size(min = 1, max = 255)
            String newName
    ) {
    }
}
