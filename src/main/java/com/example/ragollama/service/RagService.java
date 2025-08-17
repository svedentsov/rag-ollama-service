package com.example.ragollama.service;

import com.example.ragollama.config.properties.AppProperties;
import com.example.ragollama.dto.RagQueryRequest;
import com.example.ragollama.dto.RagQueryResponse;
import com.example.ragollama.dto.StreamingResponsePart;
import com.example.ragollama.entity.MessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-оркестратор RAG-конвейера, построенный на единой реактивной модели Project Reactor.
 * <p>
 * Эта версия устраняет дублирование кода путем вынесения общей логики
 * подготовки конвейера в приватный метод {@link #prepareRagFlow(RagQueryRequest)}.
 * Публичные методы теперь отвечают только за запуск общего конвейера и
 * обработку его результатов (асинхронно или в потоковом режиме).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final MultiQueryGeneratorService multiQueryGeneratorService;
    private final RetrievalService retrievalService;
    private final RerankingService rerankingService;
    private final AugmentationService augmentationService;
    private final GenerationService generationService;
    private final PromptGuardService promptGuardService;
    private final MetricService metricService;
    private final ChatHistoryService chatHistoryService;
    private final AppProperties appProperties;
    private final FusionService fusionService;

    /**
     * Контекстный объект для передачи данных по RAG-конвейеру.
     *
     * @param sessionId Идентификатор сессии.
     * @param documents Список извлеченных и переранжированных документов.
     * @param prompt    Готовый к отправке в LLM промпт.
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
                        .toFuture()
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
                                        if (!fullResponseBuilder.toString().isBlank()) {
                                            saveAssistantMessage(context.sessionId(), fullResponseBuilder.toString());
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

                    return retrieveAndRerank(request)
                            .flatMap(rerankedDocuments ->
                                    createPromptWithHistory(rerankedDocuments, request.query(), sessionId)
                                            .map(prompt -> new RagFlowContext(sessionId, rerankedDocuments, prompt))
                            )
                            .onErrorResume(e -> {
                                log.error("Критическая ошибка в RAG-конвейере для запроса '{}'", request.query(), e);
                                // Возвращаем ошибку дальше, чтобы внешние обработчики могли ее перехватить
                                return Mono.error(e);
                            });
                }));
    }

    /**
     * Выполняет Multi-Query генерацию, гибридный поиск, слияние и переранжирование.
     */
    private Mono<List<Document>> retrieveAndRerank(RagQueryRequest request) {
        return multiQueryGeneratorService.generate(request.query())
                .flatMap(queries -> {
                    Flux<List<Document>> searchResultsFlux = Flux.fromIterable(queries)
                            .flatMap(query -> retrievalService.retrieveDocuments(
                                    query, request.query(), request.topK(), request.similarityThreshold()));

                    return searchResultsFlux.collectList()
                            .map(fusionService::reciprocalRankFusion);
                })
                .map(documents -> {
                    metricService.recordRetrievedDocumentsCount(documents.size());
                    return rerankingService.rerank(documents, request.query());
                });
    }

    private Mono<Prompt> createPromptWithHistory(List<Document> documents, String query, UUID sessionId) {
        return Mono.fromCallable(() -> getChatHistory(sessionId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(chatHistory -> augmentationService.augment(documents, query, chatHistory));
    }

    private List<Message> getChatHistory(UUID sessionId) {
        int maxHistory = appProperties.chat().history().maxMessages();
        return chatHistoryService.getLastNMessages(sessionId, maxHistory);
    }

    private UUID getOrCreateSessionId(UUID sessionId) {
        return (sessionId != null) ? sessionId : UUID.randomUUID();
    }

    private void saveUserMessage(UUID sessionId, String query) {
        chatHistoryService.saveMessage(sessionId, MessageRole.USER, query);
    }

    private void saveAssistantMessage(UUID sessionId, String answer) {
        chatHistoryService.saveMessage(sessionId, MessageRole.ASSISTANT, answer);
    }
}
