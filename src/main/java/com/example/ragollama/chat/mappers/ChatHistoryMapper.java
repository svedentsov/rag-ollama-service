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

/**
 * Компонент-маппер для преобразования между доменными моделями чата и
 * DTO или внешними форматами (например, Spring AI).
 */
@Component
public class ChatHistoryMapper {

    /**
     * Преобразует список сущностей {@link ChatMessage} из БД в список
     * объектов {@link Message} для Spring AI.
     *
     * @param recentMessages Список сущностей, отсортированный по возрастанию времени.
     * @return Список сообщений для передачи в LLM.
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
     * Фабричный метод для создания новой, не сохраненной сущности {@link ChatMessage}.
     *
     * @param session  Родительская сессия.
     * @param role     Роль отправителя.
     * @param content  Текст сообщения.
     * @param parentId ID родительского сообщения.
     * @return Новая сущность {@link ChatMessage}.
     */
    public ChatMessage toChatMessageEntity(ChatSession session, MessageRole role, String content, UUID parentId) {
        return ChatMessage.builder()
                .session(session)
                .parentId(parentId)
                .role(role)
                .content(content)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Конвертирует одну сущность {@link ChatMessage} в объект {@link Message} из Spring AI.
     *
     * @param chatMessage Сущность из БД.
     * @return Сообщение в формате Spring AI.
     */
    private Message toSpringAiMessage(ChatMessage chatMessage) {
        return switch (chatMessage.getRole()) {
            case USER -> new UserMessage(chatMessage.getContent());
            case ASSISTANT -> new AssistantMessage(chatMessage.getContent());
        };
    }
}
