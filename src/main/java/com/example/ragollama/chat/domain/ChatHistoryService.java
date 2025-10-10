package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.chat.mappers.ChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления историей сообщений в чате, адаптированный для R2DBC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ChatSessionRepository chatSessionRepository;

    /**
     * Сохраняет сообщение асинхронно.
     *
     * @param session  Сессия.
     * @param role     Роль.
     * @param content  Контент.
     * @param parentId ID родителя.
     * @param taskId   ID задачи.
     * @return {@link Mono} с сохраненным сообщением.
     */
    @Transactional
    public Mono<ChatMessage> saveMessage(ChatSession session, MessageRole role, String content, UUID parentId, UUID taskId) {
        return chatSessionRepository.findById(session.getSessionId())
                .switchIfEmpty(Mono.error(new IllegalStateException("Session not found for saving message")))
                .flatMap(managedSession -> {
                    ChatMessage message = chatHistoryMapper.toChatMessageEntity(managedSession.getSessionId(), role, content, parentId, taskId);
                    return chatMessageRepository.save(message);
                })
                .doOnSuccess(saved -> log.debug("Сохранено сообщение для сессии {}: Role={}, ParentId={}, TaskId={}", session.getSessionId(), role, parentId, taskId));
    }

    /**
     * Получает последние N сообщений.
     *
     * @param sessionId ID сессии.
     * @param lastN     Количество сообщений.
     * @return {@link Mono} со списком сообщений.
     */
    @Transactional(readOnly = true)
    public Mono<List<Message>> getLastNMessages(UUID sessionId, int lastN) {
        if (lastN <= 0) {
            return Mono.just(List.of());
        }
        return chatMessageRepository.findRecentMessages(sessionId, lastN)
                .collectList()
                .map(chatHistoryMapper::toSpringAiMessages)
                .doOnSuccess(list -> log.debug("Загружено {} сообщений для сессии {}", list.size(), sessionId));
    }
}
