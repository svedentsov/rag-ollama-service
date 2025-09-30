package com.example.ragollama.chat.api.dto;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.MessageRole;

import java.util.UUID;

public record ChatMessageDto(
        UUID id,
        MessageRole role,
        String content
) {
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return new ChatMessageDto(entity.getId(), entity.getRole(), entity.getContent());
    }
}
