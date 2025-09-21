package com.example.ragollama.chat.api.dto;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.MessageRole;

public record ChatMessageDto(
        MessageRole role,
        String content
) {
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return new ChatMessageDto(entity.getRole(), entity.getContent());
    }
}
