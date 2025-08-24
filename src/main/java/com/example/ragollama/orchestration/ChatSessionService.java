package com.example.ragollama.orchestration;

import com.example.ragollama.chat.api.dto.ChatRequest;
import com.example.ragollama.chat.api.dto.ChatResponse;
import com.example.ragollama.chat.domain.ChatHistoryService;
import com.example.ragollama.chat.domain.ChatService;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.RagService;
import com.example.ragollama.shared.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-фасад, инкапсулирующий всю логику управления сессиями и историей чата.
 * <p>
 * Эта версия обновлена для передачи `sessionId` в `RagService` для
 * корректной работы аудиторского логирования.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSessionService {

    private final RagService ragService;
    private final ChatService chatService;
    private final ChatHistoryService chatHistoryService;
    private final AppProperties appProperties;
    private final AsyncTaskExecutor applicationTaskExecutor;

    /**
     * Обрабатывает RAG-запрос, управляя сессией и историей.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> processRagRequestAsync(RagQueryRequest request) {
        final UUID sessionId = getOrCreateSessionId(request.sessionId());

        return saveMessageAndGetHistory(sessionId, request.query())
                .thenCompose(history -> ragService.queryAsync(request.query(), history, request.topK(), request.similarityThreshold(), sessionId)) // <-- ПЕРЕДАЕМ sessionId
                .thenCompose(ragAnswer -> saveMessageAsync(sessionId, MessageRole.ASSISTANT, ragAnswer.answer())
                        .thenApply(v -> new RagQueryResponse(ragAnswer.answer(), ragAnswer.sourceCitations(), sessionId)));
    }

    /**
     * Обрабатывает потоковый RAG-запрос, управляя сессией и историей.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link Flux} со структурированными частями ответа.
     */
    public Flux<StreamingResponsePart> processRagRequestStream(RagQueryRequest request) {
        final UUID sessionId = getOrCreateSessionId(request.sessionId());

        return Flux.defer(() -> Mono.fromFuture(() -> saveMessageAndGetHistory(sessionId, request.query()))
                .flatMapMany(history -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return ragService.queryStream(request.query(), history, request.topK(), request.similarityThreshold(), sessionId) // <-- ПЕРЕДАЕМ sessionId
                            .doOnNext(part -> {
                                if (part instanceof StreamingResponsePart.Content content) {
                                    fullResponseBuilder.append(content.text());
                                }
                            })
                            .doOnComplete(() -> {
                                String fullResponse = fullResponseBuilder.toString();
                                if (!fullResponse.isBlank()) {
                                    saveMessageAsync(sessionId, MessageRole.ASSISTANT, fullResponse)
                                            .thenRun(() -> log.debug("Полный потоковый RAG-ответ для сессии {} сохранен.", sessionId));
                                }
                            });
                })
        );
    }

    /**
     * Обрабатывает чат-запрос, управляя сессией и историей.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link ChatResponse}.
     */
    public CompletableFuture<ChatResponse> processChatRequestAsync(ChatRequest request) {
        final UUID sessionId = getOrCreateSessionId(request.sessionId());

        return saveMessageAndGetHistory(sessionId, request.message())
                .thenCompose(history -> chatService.processChatRequestAsync(request.message(), history))
                .thenCompose(aiResponse -> saveMessageAsync(sessionId, MessageRole.ASSISTANT, aiResponse)
                        .thenApply(v -> new ChatResponse(aiResponse, sessionId)));
    }

    /**
     * Обрабатывает потоковый чат-запрос, управляя сессией и историей.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link Flux} с текстовыми частями ответа.
     */
    public Flux<String> processChatRequestStream(ChatRequest request) {
        final UUID sessionId = getOrCreateSessionId(request.sessionId());

        return Flux.defer(() -> Mono.fromFuture(() -> saveMessageAndGetHistory(sessionId, request.message()))
                .flatMapMany(history -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return chatService.processChatRequestStream(request.message(), history)
                            .doOnNext(fullResponseBuilder::append)
                            .doOnComplete(() -> {
                                String fullResponse = fullResponseBuilder.toString();
                                if (!fullResponse.isBlank()) {
                                    saveMessageAsync(sessionId, MessageRole.ASSISTANT, fullResponse)
                                            .thenRun(() -> log.debug("Полный потоковый Chat-ответ для сессии {} сохранен.", sessionId));
                                }
                            });
                })
        );
    }

    /**
     * Атомарно сохраняет сообщение пользователя и загружает актуальную историю чата.
     *
     * @param sessionId   ID сессии.
     * @param userMessage Текст сообщения пользователя.
     * @return {@link CompletableFuture} со списком сообщений.
     */
    private CompletableFuture<List<Message>> saveMessageAndGetHistory(UUID sessionId, String userMessage) {
        final UserMessage currentMessage = new UserMessage(userMessage);

        CompletableFuture<Void> saveFuture = saveMessageAsync(sessionId, MessageRole.USER, userMessage);
        CompletableFuture<List<Message>> historyFuture = getHistoryAsync(sessionId);

        return saveFuture.thenCombine(historyFuture, (v, history) -> {
            List<Message> mutableHistory = new ArrayList<>(history);
            mutableHistory.add(currentMessage);
            return mutableHistory;
        });
    }

    /**
     * Асинхронно сохраняет сообщение в базу данных.
     *
     * @param sessionId ID сессии.
     * @param role      Роль отправителя.
     * @param content   Текст сообщения.
     * @return {@link CompletableFuture}, завершающийся после сохранения.
     */
    private CompletableFuture<Void> saveMessageAsync(UUID sessionId, MessageRole role, String content) {
        return CompletableFuture.runAsync(
                () -> chatHistoryService.saveMessage(sessionId, role, content),
                applicationTaskExecutor
        ).exceptionally(ex -> {
            log.error("Не удалось сохранить сообщение для сессии {}. Роль: {}", sessionId, role, ex);
            return null;
        });
    }

    /**
     * Асинхронно загружает историю сообщений из базы данных.
     *
     * @param sessionId ID сессии.
     * @return {@link CompletableFuture} со списком сообщений.
     */
    private CompletableFuture<List<Message>> getHistoryAsync(UUID sessionId) {
        int maxHistory = appProperties.chat().history().maxMessages();
        return CompletableFuture.supplyAsync(
                () -> chatHistoryService.getLastNMessages(sessionId, maxHistory - 1),
                applicationTaskExecutor
        );
    }

    /**
     * Возвращает ID сессии из запроса или генерирует новый.
     *
     * @param sessionId Опциональный ID из DTO.
     * @return Не-null UUID.
     */
    private UUID getOrCreateSessionId(UUID sessionId) {
        return (sessionId != null) ? sessionId : UUID.randomUUID();
    }
}
