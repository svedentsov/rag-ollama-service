package com.example.ragollama.orchestration;

import com.example.ragollama.chat.domain.ChatHistoryService;
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
 * Сервис, инкапсулирующий всю общую логику управления диалогом.
 * <p>Этот сервис является результатом рефакторинга для соблюдения принципов
 * DRY (Don't Repeat Yourself) и SRP (Single Responsibility Principle). Он
 * централизует управление сессиями, загрузку истории и сохранение сообщений,
 * освобождая от этой ответственности вышестоящие сервисы-оркестраторы,
 * такие как {@link RagApplicationService} и {@link ChatApplicationService}.
 * <p>Такая декомпозиция делает систему более модульной, тестируемой и
 * легкой для понимания и поддержки.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DialogManager {

    private final ChatHistoryService chatHistoryService;
    private final AppProperties appProperties;

    /**
     * Контекстный объект, передающий состояние одного "хода" в диалоге.
     *
     * @param sessionId ID сессии.
     * @param history   Список сообщений, включая текущее.
     */
    public record TurnContext(UUID sessionId, List<Message> history) {
    }

    /**
     * Начинает новый "ход" в диалоге.
     * <p>Этот метод атомарно выполняет три действия:
     * <ol>
     *     <li>Определяет или создает ID сессии.</li>
     *     <li>Асинхронно сохраняет текущее сообщение пользователя.</li>
     *     <li>Асинхронно загружает релевантную историю чата.</li>
     * </ol>
     * Он возвращает {@link TurnContext}, содержащий все необходимое для
     * последующего вызова бизнес-логики (например, RAG-пайплайна).
     *
     * @param sessionId   Опциональный ID сессии из запроса.
     * @param userMessage Текст сообщения пользователя.
     * @param role        Роль отправителя.
     * @return {@link CompletableFuture} с контекстом текущего хода.
     */
    public CompletableFuture<TurnContext> startTurn(UUID sessionId, String userMessage, MessageRole role) {
        final UUID finalSessionId = (sessionId != null) ? sessionId : UUID.randomUUID();
        final UserMessage currentMessage = new UserMessage(userMessage);

        CompletableFuture<Void> saveFuture = saveMessageAsync(finalSessionId, role, userMessage);
        CompletableFuture<List<Message>> historyFuture = getHistoryAsync(finalSessionId);

        return saveFuture.thenCombine(historyFuture, (v, history) -> {
            List<Message> fullHistory = new ArrayList<>(history);
            fullHistory.add(currentMessage);
            return new TurnContext(finalSessionId, fullHistory);
        });
    }

    /**
     * Завершает "ход" в диалоге, асинхронно сохраняя ответ ассистента.
     *
     * @param sessionId ID сессии.
     * @param content   Текст ответа ассистента.
     * @param role      Роль (всегда ASSISTANT).
     * @return {@link CompletableFuture}, который завершается после сохранения.
     */
    public CompletableFuture<Void> endTurn(UUID sessionId, String content, MessageRole role) {
        return saveMessageAsync(sessionId, role, content);
    }

    /**
     * Асинхронно сохраняет сообщение, инкапсулируя обработку ошибок.
     *
     * @param sessionId ID сессии.
     * @param role      Роль отправителя.
     * @param content   Текст сообщения.
     * @return {@link CompletableFuture}, который завершается после попытки сохранения.
     */
    private CompletableFuture<Void> saveMessageAsync(UUID sessionId, MessageRole role, String content) {
        return chatHistoryService.saveMessageAsync(sessionId, role, content)
                .exceptionally(ex -> {
                    log.error("Не удалось сохранить сообщение для сессии {}. Роль: {}", sessionId, role, ex);
                    // Игнорируем ошибку, чтобы не прерывать основной поток ответа пользователю.
                    // Это сознательное архитектурное решение для повышения отказоустойчивости.
                    return null;
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
        // Загружаем N-1 сообщений, так как текущее сообщение пользователя будет добавлено вручную.
        return chatHistoryService.getLastNMessagesAsync(sessionId, maxHistory - 1);
    }
}
