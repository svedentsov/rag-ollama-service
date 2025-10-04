package com.example.ragollama.orchestration;

import com.example.ragollama.chat.domain.ChatMessageRepository;
import com.example.ragollama.chat.domain.ChatHistoryService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-оркестратор, управляющий жизненным циклом одного "хода" в диалоге.
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

    /**
     * Начинает новый "ход" диалога: сохраняет или переиспользует сообщение пользователя и загружает историю.
     *
     * @param sessionId ID сессии (может быть null для новой).
     * @param userMessage Текст сообщения от пользователя.
     * @param role Роль (всегда USER).
     * @return CompletableFuture с TurnContext.
     */
    public CompletableFuture<TurnContext> startTurn(UUID sessionId, String userMessage, MessageRole role) {
        final ChatSession session = chatSessionService.findOrCreateSession(sessionId);
        final UUID finalSessionId = session.getSessionId();
        final UserMessage currentMessage = new UserMessage(userMessage);

        Optional<ChatMessage> lastMessageOpt = chatMessageRepository.findTopBySessionSessionIdOrderByCreatedAtDesc(finalSessionId);

        CompletableFuture<ChatMessage> userMessageFuture;
        if (lastMessageOpt.isPresent() && lastMessageOpt.get().getRole() == MessageRole.USER && lastMessageOpt.get().getContent().equals(userMessage)) {
            log.debug("Переиспользование существующего сообщения пользователя (ID: {}) для сессии {}", lastMessageOpt.get().getId(), finalSessionId);
            userMessageFuture = CompletableFuture.completedFuture(lastMessageOpt.get());
        } else {
            userMessageFuture = saveMessageAsync(session, role, userMessage, null, null);
        }

        CompletableFuture<List<Message>> historyFuture = getHistoryAsync(finalSessionId);

        return userMessageFuture.thenCombine(historyFuture, (userMessageEntity, history) -> {
            List<Message> fullHistory = new ArrayList<>(history);
            fullHistory.add(currentMessage);
            return new TurnContext(finalSessionId, userMessageEntity.getId(), fullHistory);
        });
    }

    public CompletableFuture<Void> endTurn(UUID sessionId, UUID parentMessageId, String content, MessageRole role, UUID taskId) {
        ChatSession session = chatSessionService.findAndVerifyOwnership(sessionId);
        return saveMessageAsync(session, role, content, parentMessageId, taskId).thenApply(v -> null);
    }

    private CompletableFuture<ChatMessage> saveMessageAsync(ChatSession session, MessageRole role, String content, UUID parentId, UUID taskId) {
        return chatHistoryService.saveMessageAsync(session, role, content, parentId, taskId)
                .exceptionally(ex -> {
                    log.error("Не удалось сохранить сообщение для сессии {}. Роль: {}", session.getSessionId(), role, ex);
                    throw new RuntimeException("Ошибка сохранения сообщения", ex);
                });
    }

    private CompletableFuture<List<Message>> getHistoryAsync(UUID sessionId) {
        int maxHistory = appProperties.chat().history().maxMessages();
        return chatHistoryService.getLastNMessagesAsync(sessionId, maxHistory - 1);
    }
}
