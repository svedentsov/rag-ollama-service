package com.example.ragollama.orchestration;

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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-оркестратор, инкапсулирующий логику управления одним "ходом" в диалоге.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DialogManager {

    public record TurnContext(UUID sessionId, UUID userMessageId, List<Message> history) {
    }

    private final ChatHistoryService chatHistoryService;
    private final ChatSessionService chatSessionService;
    private final AppProperties appProperties;

    public CompletableFuture<TurnContext> startTurn(UUID sessionId, String userMessage, MessageRole role) {
        final ChatSession session = chatSessionService.findOrCreateSession(sessionId);
        final UUID finalSessionId = session.getSessionId();
        final UserMessage currentMessage = new UserMessage(userMessage);
        CompletableFuture<ChatMessage> saveFuture = saveMessageAsync(session, role, userMessage, null);
        CompletableFuture<List<Message>> historyFuture = getHistoryAsync(finalSessionId);
        return saveFuture.thenCombine(historyFuture, (savedUserMessage, history) -> {
            List<Message> fullHistory = new ArrayList<>(history);
            fullHistory.add(currentMessage);
            return new TurnContext(finalSessionId, savedUserMessage.getId(), fullHistory);
        });
    }

    public CompletableFuture<TurnContext> startRegenerationTurn(UUID sessionId, UUID parentMessageId) {
        final ChatSession session = chatSessionService.findAndVerifyOwnership(sessionId);
        CompletableFuture<List<Message>> historyFuture = getHistoryAsync(sessionId);
        return historyFuture.thenApply(history -> new TurnContext(sessionId, parentMessageId, history));
    }

    public CompletableFuture<Void> endTurn(UUID sessionId, UUID parentMessageId, String content, MessageRole role) {
        ChatSession session = chatSessionService.findAndVerifyOwnership(sessionId);
        return saveMessageAsync(session, role, content, parentMessageId).thenApply(v -> null);
    }

    private CompletableFuture<ChatMessage> saveMessageAsync(ChatSession session, MessageRole role, String content, UUID parentId) {
        return chatHistoryService.saveMessageAsync(session, role, content, parentId)
                .exceptionally(ex -> {
                    log.error("Не удалось сохранить сообщение для сессии {}. Роль: {}", session.getSessionId(), role, ex);
                    throw new RuntimeException("Ошибка сохранения сообщения", ex);
                });
    }

    /**
     * Асинхронно загружает историю чата на основе настроек.
     *
     * @param sessionId ID сессии.
     * @return {@link CompletableFuture} со списком сообщений.
     */
    private CompletableFuture<List<Message>> getHistoryAsync(UUID sessionId) {
        int maxHistory = appProperties.chat().history().maxMessages();
        return chatHistoryService.getLastNMessagesAsync(sessionId, maxHistory - 1);
    }
}
