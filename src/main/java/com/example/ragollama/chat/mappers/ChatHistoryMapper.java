package com.example.ragollama.chat.mappers;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.MessageRole;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Компонент-маппер, отвечающий за преобразование данных между доменной
 * моделью истории чата и DTO-объектами библиотеки Spring AI.
 * <p>
 * Изоляция этой логики в отдельном классе повышает тестируемость,
 * соответствует Принципу единственной ответственности и упрощает
 * поддержку кода.
 */
@Component
public class ChatHistoryMapper {

    /**
     * Преобразует список доменных сущностей {@link ChatMessage} в список
     * DTO {@link Message} для Spring AI.
     * <p>
     * Метод также выполняет реверсирование списка, чтобы сообщения
     * были упорядочены в правильном хронологическом порядке (от старых к новым)
     * для корректной передачи в LLM.
     *
     * @param recentMessages Список сущностей из БД, обычно отсортированный
     *                       по убыванию даты создания (от новых к старым).
     * @return Хронологически упорядоченный список {@link Message}.
     */
    public List<Message> toSpringAiMessages(List<ChatMessage> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return Collections.emptyList();
        }

        List<Message> springAiMessages = recentMessages.stream()
                .map(this::toSpringAiMessage)
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.reverse(springAiMessages);
        return springAiMessages;
    }

    /**
     * Создает новую, не сохраненную в БД, сущность {@link ChatMessage}
     * из "сырых" данных.
     * <p>
     * Этот метод инкапсулирует логику создания доменного объекта,
     * освобождая от этой ответственности сервисный слой. Он использует
     * {@link OffsetDateTime#now()} для обеспечения корректности
     * временных меток с учетом часовых поясов.
     *
     * @param sessionId ID сессии чата.
     * @param role      Роль отправителя сообщения.
     * @param content   Текст сообщения.
     * @return Готовый к сохранению объект {@link ChatMessage}.
     */
    public ChatMessage toChatMessageEntity(UUID sessionId, MessageRole role, String content) {
        return ChatMessage.builder()
                .sessionId(sessionId)
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
