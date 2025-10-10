package com.example.ragollama.chat.mappers;

import com.example.ragollama.chat.domain.model.ChatMessage;
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

/**
 * Компонент-маппер для преобразования между доменными моделями чата и моделями Spring AI.
 */
@Component
public class ChatHistoryMapper {

    /**
     * Преобразует список доменных сущностей сообщений в список сообщений формата Spring AI.
     *
     * @param recentMessages Список доменных сущностей.
     * @return Список сообщений Spring AI.
     */
    public List<Message> toSpringAiMessages(List<ChatMessage> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return Collections.emptyList();
        }
        return recentMessages.stream()
                .map(this::toSpringAiMessage)
                .collect(Collectors.toList());
    }

    /**
     * Создает новую сущность ChatMessage для сохранения в БД.
     *
     * @param sessionId ID сессии.
     * @param role      Роль отправителя.
     * @param content   Текст сообщения.
     * @param parentId  ID родительского сообщения.
     * @param taskId    ID связанной задачи.
     * @return Новая, не сохраненная сущность ChatMessage.
     */
    public ChatMessage toChatMessageEntity(UUID sessionId, MessageRole role, String content, UUID parentId, UUID taskId) {
        return ChatMessage.builder()
                .sessionId(sessionId)
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
