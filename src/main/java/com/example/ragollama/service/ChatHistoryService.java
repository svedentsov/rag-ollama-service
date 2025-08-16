package com.example.ragollama.service;

import com.example.ragollama.entity.ChatMessage;
import com.example.ragollama.entity.MessageRole;
import com.example.ragollama.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Сохраняет одно сообщение в базу данных.
     */
    @Transactional
    public void saveMessage(UUID sessionId, MessageRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(message);
        log.debug("Сохранено сообщение для сессии {}: Role={}", sessionId, role);
    }

    /**
     * Загружает N последних сообщений для указанной сессии.
     * Результат преобразуется в список объектов {@link Message}, совместимых со Spring AI.
     *
     * @param sessionId ID сессии чата.
     * @param lastN     Количество последних сообщений для загрузки.
     * @return Список сообщений, отсортированных от старых к новым.
     */
    @Transactional(readOnly = true)
    public List<Message> getLastNMessages(UUID sessionId, int lastN) {
        // Используем PageRequest для эффективного LIMIT-запроса в БД
        PageRequest pageRequest = PageRequest.of(0, lastN, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageRequest);

        log.debug("Загружено {} сообщений для сессии {}", recentMessages.size(), sessionId);

        // Преобразуем сущности БД в DTO Spring AI и разворачиваем в правильный хронологический порядок
        return recentMessages.stream()
                .map(this::toSpringAiMessage)
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    java.util.Collections.reverse(list);
                    return list;
                }));
    }

    /**
     * Конвертирует нашу сущность {@link ChatMessage} в объект {@link Message} из Spring AI.
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
