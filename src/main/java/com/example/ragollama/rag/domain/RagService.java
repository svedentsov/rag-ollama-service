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
 * <p>
 * Класс реализует паттерн "Фасад", являясь единой точкой входа для RAG-логики.
 * Он делегирует выполнение каждого этапа конвейера специализированным,
 * инкапсулированным компонентам.
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
     * Внутренний record для передачи подготовленного контекста по RAG-конвейеру.
     *
     * @param sessionId Идентификатор сессии.
     * @param documents Список извлеченных и релевантных документов.
     * @param prompt    Финальный промпт, готовый для отправки в LLM.
     */
    private record RagFlowContext(UUID sessionId, List<Document> documents, Prompt prompt) {
    }

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
     * Корректно обрабатывает ошибки внутри потока, преобразуя их в событие {@link StreamingResponsePart.Error}.
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
                        .onErrorResume(e -> {
                            log.error("Ошибка в потоке RAG для запроса '{}': {}", request.query(), e.getMessage());
                            return Flux.just(new StreamingResponsePart.Error("Произошла внутренняя ошибка при обработке вашего запроса."));
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

                    return queryProcessingPipeline.process(request.query())
                            .flatMap(enhancedQueries -> retrievalStrategy.retrieve(enhancedQueries, request.query()))
                            .flatMap(rerankedDocuments -> {
                                Mono<Prompt> promptMono = createPromptWithHistory(rerankedDocuments, request.query(), sessionId);
                                return promptMono.map(prompt -> new RagFlowContext(sessionId, rerankedDocuments, prompt));
                            });
                }))
                .onErrorResume(e -> {
                    log.error("Критическая ошибка на этапе подготовки RAG-конвейера для запроса '{}'", request.query(), e);
                    return Mono.error(e);
                });
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
        return Mono.fromFuture(() -> getChatHistoryAsync(sessionId))
                .flatMap(chatHistory -> augmentationService.augment(documents, query, chatHistory));
    }

    private CompletableFuture<List<Message>> getChatHistoryAsync(UUID sessionId) {
        int maxHistory = appProperties.chat().history().maxMessages();
        return chatHistoryService.getLastNMessagesAsync(sessionId, maxHistory);
    }

    private UUID getOrCreateSessionId(UUID sessionId) {
        return (sessionId != null) ? sessionId : UUID.randomUUID();
    }

    private void saveUserMessage(UUID sessionId, String query) {
        chatHistoryService.saveMessageAsync(sessionId, MessageRole.USER, query)
                .exceptionally(e -> {
                    log.warn("Не удалось сохранить сообщение пользователя для сессии {}: {}", sessionId, e.getMessage());
                    return null;
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
