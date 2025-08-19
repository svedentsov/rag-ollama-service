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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Сервис для управления историей сообщений чата с корректной асинхронностью.
 * В этой версии исправлена ошибка {@link UnsupportedOperationException}
 * путем замены неизменяемой коллекции, возвращаемой {@code .toList()}, на
 * изменяемый {@link ArrayList}, создаваемый через {@code Collectors.toList()}.
 * Это позволяет безопасно выполнять операцию {@code Collections.reverse()}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Асинхронно и транзакционно сохраняет одно сообщение в базу данных.
     *
     * @param sessionId ID сессии чата.
     * @param role      Роль отправителя сообщения.
     * @param content   Текст сообщения.
     * @return {@link CompletableFuture}, который Spring автоматически создаст и завершит.
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public CompletableFuture<Void> saveMessageAsync(UUID sessionId, MessageRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(message);
        log.debug("Сохранено сообщение для сессии {}: Role={}", sessionId, role);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Асинхронно и транзакционно загружает N последних сообщений для указанной сессии.
     *
     * @param sessionId ID сессии чата.
     * @param lastN     Количество последних сообщений для загрузки.
     * @return {@link CompletableFuture}, который по завершении будет содержать список сообщений.
     */
    @Async("applicationTaskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<List<Message>> getLastNMessagesAsync(UUID sessionId, int lastN) {
        PageRequest pageRequest = PageRequest.of(0, lastN, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageRequest);
        log.debug("Загружено {} сообщений для сессии {}", recentMessages.size(), sessionId);
        if (recentMessages.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        List<Message> springAiMessages = recentMessages.stream()
                .map(this::toSpringAiMessage)
                .collect(Collectors.toList());
        Collections.reverse(springAiMessages);
        return CompletableFuture.completedFuture(springAiMessages);
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
