package com.example.ragollama.chat.api.dto;

import com.example.ragollama.chat.domain.model.ChatSession;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatSessionDto(
        UUID sessionId,
        String chatName,
        String lastMessageContent,
        OffsetDateTime lastMessageTimestamp
) {
    public static ChatSessionDto fromEntity(ChatSession entity) {
        String content = (entity.getLastMessage() != null) ? entity.getLastMessage().getContent() : null;
        OffsetDateTime timestamp = (entity.getLastMessage() != null) ? entity.getLastMessage().getCreatedAt() : entity.getUpdatedAt();

        return new ChatSessionDto(
                entity.getSessionId(),
                entity.getChatName(),
                content,
                timestamp
        );
    }

    // !!! ИЗМЕНЕНИЕ: Добавлен DTO для запроса на обновление имени.
    @Schema(description = "DTO для обновления имени чата")
    public record UpdateRequest(
            @NotBlank @Size(min = 1, max = 255)
            String newName
    ) {
    }
}
