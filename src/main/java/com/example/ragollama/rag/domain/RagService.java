package com.example.ragollama.rag.domain;

import com.example.ragollama.chat.domain.ChatHistoryService;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.security.PromptGuardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-оркестратор RAG-конвейера, построенный на единой реактивной модели.
 * Класс реализует паттерн "Фасад", являясь единой точкой входа для RAG-логики.
 * Он делегирует выполнение каждого этапа конвейера специализированным,
 * инкапсулированным компонентам, таким как {@link QueryProcessingPipeline} и
 * {@link HybridRetrievalStrategy}. Это делает архитектуру модульной, тестируемой
 * и соответствующей принципам SOLID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final QueryProcessingPipeline queryProcessingPipeline;
    private final HybridRetrievalStrategy retrievalStrategy;
    private final AugmentationService augmentationService;
    private final GenerationService generationService;
    private final PromptGuardService promptGuardService;
    private final MetricService metricService;
    private final ChatHistoryService chatHistoryService;
    private final AppProperties appProperties;

    /**
     * Контекстный объект для передачи данных по RAG-конвейеру.
     */
    private record RagFlowContext(UUID sessionId, List<Document> documents, Prompt prompt) {}

    /**
     * Выполняет RAG-запрос и возвращает полный ответ после его генерации.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> queryAsync(RagQueryRequest request) {
        return metricService.recordTimer("rag.requests.async",
                () -> prepareRagFlow(request)
                        .flatMap(context -> Mono.fromFuture(
                                generationService.generate(context.prompt(), context.documents(), context.sessionId())
                        ))
                        .doOnSuccess(response -> saveAssistantMessage(response.sessionId(), response.answer()))
                        .toFuture() // В самом конце конвертируем Mono в CompletableFuture для контроллера
        );
    }

    /**
     * Выполняет RAG-запрос и возвращает ответ в виде потока (SSE).
     *
     * @param request DTO с запросом от пользователя.
     * @return Реактивный поток {@link Flux} со структурированными событиями.
     */
    public Flux<StreamingResponsePart> queryStream(RagQueryRequest request) {
        return metricService.recordTimer("rag.requests.stream",
                () -> prepareRagFlow(request)
                        .flatMapMany(context -> {
                            final StringBuilder fullResponseBuilder = new StringBuilder();
                            return generationService.generateStructuredStream(context.prompt(), context.documents(), context.sessionId())
                                    .doOnNext(part -> {
                                        if (part instanceof StreamingResponsePart.Content content) {
                                            fullResponseBuilder.append(content.text());
                                        }
                                    })
                                    .doOnComplete(() -> {
                                        String fullResponse = fullResponseBuilder.toString();
                                        if (!fullResponse.isBlank()) {
                                            saveAssistantMessage(context.sessionId(), fullResponse);
                                        }
                                    });
                        })
        );
    }

    /**
     * Собирает и выполняет общую часть RAG-конвейера: от валидации до создания промпта.
     *
     * @param request Входящий запрос.
     * @return {@link Mono} с подготовленным контекстом {@link RagFlowContext}.
     */
    private Mono<RagFlowContext> prepareRagFlow(RagQueryRequest request) {
        return Mono.fromRunnable(() -> promptGuardService.checkForInjection(request.query()))
                .then(Mono.defer(() -> {
                    final UUID sessionId = getOrCreateSessionId(request.sessionId());
                    saveUserMessage(sessionId, request.query());
                    return retrieveDocuments(request)
                            .flatMap(rerankedDocuments ->
                                    createPromptWithHistory(rerankedDocuments, request.query(), sessionId)
                                            .map(prompt -> new RagFlowContext(sessionId, rerankedDocuments, prompt))
                            );
                }))
                .onErrorResume(e -> {
                    log.error("Критическая ошибка в RAG-конвейере для запроса '{}'", request.query(), e);
                    return Mono.error(e);
                });
    }

    /**
     * Выполняет этап извлечения (Retrieval) в RAG-конвейере.
     *
     * @param request DTO с оригинальным запросом.
     * @return {@link Mono} с финальным списком релевантных документов.
     */
    private Mono<List<Document>> retrieveDocuments(RagQueryRequest request) {
        // queryProcessingPipeline.process возвращает Mono, используем flatMap
        return queryProcessingPipeline.process(request.query())
                .flatMap(queries -> retrievalStrategy.retrieve(queries, request.query()))
                .doOnNext(documents -> metricService.recordRetrievedDocumentsCount(documents.size()));
    }

    /**
     * Асинхронно создает промпт, обогащенный историей чата.
     *
     * @param documents Список извлеченных документов.
     * @param query     Текущий запрос пользователя.
     * @param sessionId ID сессии.
     * @return {@link Mono}, который по завершении будет содержать готовый {@link Prompt}.
     */
    private Mono<Prompt> createPromptWithHistory(List<Document> documents, String query, UUID sessionId) {
        // Используем Mono.fromFuture для моста между CompletableFuture и Mono
        return Mono.fromFuture(() -> getChatHistoryAsync(sessionId))
                .flatMap(chatHistory -> augmentationService.augment(documents, query, chatHistory));
    }

    private CompletableFuture<List<Message>> getChatHistoryAsync(UUID sessionId) {
        int maxHistory = appProperties.chat().history().maxMessages();
        // Вызываем правильный асинхронный метод
        return chatHistoryService.getLastNMessagesAsync(sessionId, maxHistory);
    }

    private UUID getOrCreateSessionId(UUID sessionId) {
        return (sessionId != null) ? sessionId : UUID.randomUUID();
    }

    private void saveUserMessage(UUID sessionId, String query) {
        // Вызываем асинхронный метод и обрабатываем возможную ошибку, не блокируя поток
        chatHistoryService.saveMessageAsync(sessionId, MessageRole.USER, query)
                .exceptionally(e -> {
                    log.warn("Не удалось сохранить сообщение пользователя для сессии {}: {}", sessionId, e.getMessage());
                    return null; // Возвращаем null, чтобы завершить exceptionally блок
                });
    }

    private void saveAssistantMessage(UUID sessionId, String answer) {
        chatHistoryService.saveMessageAsync(sessionId, MessageRole.ASSISTANT, answer)
                .exceptionally(e -> {
                    log.warn("Не удалось сохранить сообщение ассистента для сессии {}: {}", sessionId, e.getMessage());
                    return null;
                });
    }
}
