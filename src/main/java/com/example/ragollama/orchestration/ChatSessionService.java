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
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.shared.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Сервис-фасад, инкапсулирующий всю логику управления сессиями и историей чата.
 * <p>
 * Этот класс является высокоуровневым оркестратором, который связывает
 * контроллеры с доменными сервисами (`RagService`, `ChatService`). Он отвечает за:
 * <ul>
 *     <li>Управление жизненным циклом сессии (создание/получение ID).</li>
 *     <li>Атомарное сохранение сообщений пользователя и ассистента в историю.</li>
 *     <li>Извлечение истории сообщений для передачи в LLM.</li>
 *     <li>Вызов соответствующего доменного сервиса для выполнения бизнес-логики.</li>
 *     <li>Формирование финального DTO ответа для контроллера.</li>
 * </ul>
 * Внутренняя реализация использует универсальный приватный метод-оркестратор
 * для асинхронных запросов, чтобы устранить дублирование кода и следовать принципу DRY.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSessionService {

    private final RagService ragService;
    private final ChatService chatService;
    private final ChatHistoryService chatHistoryService;
    private final AppProperties appProperties;

    /**
     * Обрабатывает RAG-запрос, управляя сессией и историей.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> processRagRequestAsync(RagQueryRequest request) {
        return orchestrateAsyncRequest(
                request.sessionId(),
                request.query(),
                // Функция, выполняющая основную бизнес-логику
                (query, history) -> ragService.queryAsync(query, history, request.topK(), request.similarityThreshold(), getOrCreateSessionId(request.sessionId())),
                // Функция для извлечения текстового ответа из доменного объекта
                RagAnswer::answer,
                // Функция для создания финального DTO ответа
                (ragAnswer, sessionId) -> new RagQueryResponse(ragAnswer.answer(), ragAnswer.sourceCitations(), sessionId)
        );
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
                    return ragService.queryStream(request.query(), history, request.topK(), request.similarityThreshold(), sessionId)
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
        return orchestrateAsyncRequest(
                request.sessionId(),
                request.message(),
                chatService::processChatRequestAsync,
                Function.identity(),
                ChatResponse::new
        );
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
     * Универсальный метод-оркестратор для асинхронных (не-потоковых) запросов.
     *
     * @param sessionId       ID сессии из запроса.
     * @param userMessage     Сообщение пользователя.
     * @param logicExecutor   Функция, выполняющая основную бизнес-логику (вызов RAG или Chat сервиса).
     * @param answerExtractor Функция для извлечения текстового ответа из доменного объекта.
     * @param responseCreator Функция для создания финального DTO ответа.
     * @param <T>             Тип доменного ответа (например, {@link RagAnswer} или {@link String}).
     * @param <R>             Тип финального DTO ответа (например, {@link RagQueryResponse} или {@link ChatResponse}).
     * @return {@link CompletableFuture} с финальным DTO.
     */
    private <T, R> CompletableFuture<R> orchestrateAsyncRequest(
            UUID sessionId,
            String userMessage,
            BiFunction<String, List<Message>, CompletableFuture<T>> logicExecutor,
            Function<T, String> answerExtractor,
            BiFunction<T, UUID, R> responseCreator) {

        final UUID finalSessionId = getOrCreateSessionId(sessionId);
        return saveMessageAndGetHistory(finalSessionId, userMessage)
                .thenCompose(history -> logicExecutor.apply(userMessage, history))
                .thenCompose(domainResponse -> saveMessageAsync(finalSessionId, MessageRole.ASSISTANT, answerExtractor.apply(domainResponse))
                        .thenApply(v -> responseCreator.apply(domainResponse, finalSessionId)));
    }

    /**
     * Атомарно сохраняет сообщение пользователя и загружает актуальную историю чата.
     *
     * @param sessionId   ID сессии.
     * @param userMessage Текст сообщения пользователя.
     * @return {@link CompletableFuture} со списком сообщений, включающим текущее.
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
        return chatHistoryService.saveMessageAsync(sessionId, role, content)
                .exceptionally(ex -> {
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
        return chatHistoryService.getLastNMessagesAsync(sessionId, maxHistory - 1);
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
