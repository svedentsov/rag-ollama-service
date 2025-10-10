package com.example.ragollama.orchestration;

import com.example.ragollama.chat.domain.ChatHistoryService;
import com.example.ragollama.chat.domain.ChatMessageRepository;
import com.example.ragollama.chat.domain.ChatSessionService;
import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.shared.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сервис-оркестратор для управления "ходом" в диалоге, адаптированный для R2DBC.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DialogManager {

    public record TurnContext(UUID sessionId, UUID userMessageId, List<Message> history) {}

    private final ChatHistoryService chatHistoryService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageRepository chatMessageRepository;
    private final AppProperties appProperties;

    public Mono<TurnContext> startTurn(UUID sessionId, String userMessage, MessageRole role) {
        Mono<ChatSession> sessionMono = chatSessionService.findOrCreateSession(sessionId);
        UserMessage currentMessage = new UserMessage(userMessage);

        return sessionMono.flatMap(session -> {
            final UUID finalSessionId = session.getSessionId();
            Mono<ChatMessage> userMessageMono = chatMessageRepository.findTopBySessionIdOrderByCreatedAtDesc(finalSessionId)
                    .filter(lastMessage -> lastMessage.getRole() == MessageRole.USER && lastMessage.getContent().equals(userMessage))
                    .doOnNext(reused -> log.debug("Переиспользование сообщения (ID: {}) для сессии {}", reused.getId(), finalSessionId))
                    .switchIfEmpty(saveMessage(session, role, userMessage, null, null));

            Mono<List<Message>> historyMono = getHistory(finalSessionId);

            return Mono.zip(userMessageMono, historyMono)
                    .map(tuple -> {
                        List<Message> fullHistory = new ArrayList<>(tuple.getT2());
                        fullHistory.add(currentMessage);
                        return new TurnContext(finalSessionId, tuple.getT1().getId(), fullHistory);
                    });
        });
    }

    public Mono<Void> endTurn(UUID sessionId, UUID parentMessageId, String content, MessageRole role, UUID taskId) {
        return chatSessionService.findAndVerifyOwnership(sessionId)
                .flatMap(session -> saveMessage(session, role, content, parentMessageId, taskId))
                .then();
    }

    private Mono<ChatMessage> saveMessage(ChatSession session, MessageRole role, String content, UUID parentId, UUID taskId) {
        return chatHistoryService.saveMessage(session, role, content, parentId, taskId)
                .onErrorMap(ex -> {
                    log.error("Не удалось сохранить сообщение для сессии {}. Роль: {}", session.getSessionId(), role, ex);
                    return new RuntimeException("Ошибка сохранения сообщения", ex);
                });
    }

    private Mono<List<Message>> getHistory(UUID sessionId) {
        int maxHistory = appProperties.chat().history().maxMessages();
        return chatHistoryService.getLastNMessages(sessionId, maxHistory - 1);
    }
}
