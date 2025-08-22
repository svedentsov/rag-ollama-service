package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.MessageRole;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для управления историей сообщений чата.
 * <p>
 * Реализует синхронные, транзакционные методы для работы с базой данных.
 * Этот сервис не управляет асинхронностью; он предоставляет чистый API
 * для CRUD-операций, делегируя управление потоками вызывающему слою.
 * Это упрощает тестирование и четко разделяет ответственности.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Синхронно и транзакционно сохраняет одно сообщение в базу данных.
     *
     * @param sessionId ID сессии чата.
     * @param role      Роль отправителя сообщения.
     * @param content   Текст сообщения.
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
     * Синхронно и транзакционно загружает N последних сообщений для указанной сессии.
     * <p>
     * Результат возвращается в обратном хронологическом порядке (от старых к новым),
     * что является корректным форматом для передачи в Spring AI Prompt.
     *
     * @param sessionId ID сессии чата.
     * @param lastN     Количество последних сообщений для загрузки.
     * @return Список сообщений {@link Message}, готовый к использованию в промпте.
     */
    @Transactional(readOnly = true)
    public List<Message> getLastNMessages(UUID sessionId, int lastN) {
        PageRequest pageRequest = PageRequest.of(0, lastN, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageRequest);
        log.debug("Загружено {} сообщений для сессии {}", recentMessages.size(), sessionId);

        if (recentMessages.isEmpty()) {
            return Collections.emptyList();
        }

        List<Message> springAiMessages = recentMessages.stream()
                .map(this::toSpringAiMessage)
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.reverse(springAiMessages);
        return springAiMessages;
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
