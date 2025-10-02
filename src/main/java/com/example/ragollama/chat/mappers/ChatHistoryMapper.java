package com.example.ragollama.chat.mappers;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.chat.domain.model.MessageRole;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ChatHistoryMapper {

    public List<Message> toSpringAiMessages(List<ChatMessage> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return Collections.emptyList();
        }
        return recentMessages.stream()
                .map(this::toSpringAiMessage)
                .collect(Collectors.toList());
    }

    /**
     * Фабричный метод для создания новой сущности ChatMessage.
     * @param session  Родительская сессия.
     * @param role     Роль отправителя.
     * @param content  Текст сообщения.
     * @param parentId ID родительского сообщения.
     * @param taskId   ID задачи, сгенерировавшей сообщение.
     * @return Новая сущность ChatMessage.
     */
    public ChatMessage toChatMessageEntity(ChatSession session, MessageRole role, String content, UUID parentId, UUID taskId) {
        return ChatMessage.builder()
                .session(session)
                .parentId(parentId)
                .taskId(taskId)
                .role(role)
                .content(content)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private Message toSpringAiMessage(ChatMessage chatMessage) {
        return switch (chatMessage.getRole()) {
            case USER -> new UserMessage(chatMessage.getContent());
            case ASSISTANT -> new AssistantMessage(chatMessage.getContent());
        };
    }
}
