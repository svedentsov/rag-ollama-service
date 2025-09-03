package com.example.ragollama.rag.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.optimization.KnowledgeRouterAgent;
import com.example.ragollama.rag.agent.ProcessedQueries;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.domain.reranking.RerankingService;
import com.example.ragollama.rag.postprocessing.RagPostProcessingOrchestrator;
import com.example.ragollama.rag.postprocessing.RagProcessingContext;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.security.PromptGuardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * "Чистый" сервис-оркестратор RAG-конвейера с интеллектуальной маршрутизацией.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final QueryProcessingPipeline queryProcessingPipeline;
    private final HybridRetrievalStrategy retrievalStrategy;
    private final RerankingService rerankingService;
    private final AugmentationService augmentationService;
    private final GenerationService generationService;
    private final PromptGuardService promptGuardService;
    private final MetricService metricService;
    private final RagPostProcessingOrchestrator postProcessingOrchestrator;
    private final KnowledgeRouterAgent knowledgeRouterAgent;

    /**
     * Асинхронно выполняет полный RAG-запрос (не-потоковый).
     *
     * @param query               Запрос пользователя.
     * @param history             История чата для контекста.
     * @param topK                Количество извлекаемых документов.
     * @param similarityThreshold Порог схожести.
     * @param sessionId           Идентификатор сессии для аудита.
     * @return {@link CompletableFuture} с финальным ответом в виде {@link RagAnswer}.
     */
    public CompletableFuture<RagAnswer> queryAsync(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        final String requestId = MDC.get("requestId");
        return metricService.recordTimer("rag.requests.async.pure",
                () -> prepareRagFlow(query, history, topK, similarityThreshold)
                        .flatMap(context ->
                                Mono.fromFuture(generationService.generate(context.prompt(), context.documents()))
                                        .doOnSuccess(response -> {
                                            var processingContext = new RagProcessingContext(
                                                    requestId, query, context.documents(), context.prompt(),
                                                    response, sessionId);
                                            postProcessingOrchestrator.process(processingContext);
                                        })
                        )
                        .toFuture()
        );
    }

    /**
     * Выполняет полный RAG-запрос в потоковом режиме (Server-Sent Events).
     *
     * @param query               Запрос пользователя.
     * @param history             История чата.
     * @param topK                Количество извлекаемых документов.
     * @param similarityThreshold Порог схожести.
     * @param sessionId           Идентификатор сессии для аудита.
     * @return Реактивный поток {@link Flux} со структурированными частями ответа.
     */
    public Flux<StreamingResponsePart> queryStream(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        final String requestId = MDC.get("requestId");
        var documentsRef = new AtomicReference<List<Document>>();
        var promptRef = new AtomicReference<Prompt>();
        var fullAnswerBuilder = new StringBuilder();

        return metricService.recordTimer("rag.requests.stream.pure",
                () -> prepareRagFlow(query, history, topK, similarityThreshold)
                        .doOnNext(context -> {
                            documentsRef.set(context.documents());
                            promptRef.set(context.prompt());
                        })
                        .flatMapMany(context -> generationService.generateStructuredStream(context.prompt(), context.documents()))
                        .doOnNext(part -> {
                            if (part instanceof StreamingResponsePart.Content content) {
                                fullAnswerBuilder.append(content.text());
                            }
                        })
                        .doOnComplete(() -> {
                            String finalAnswer = fullAnswerBuilder.toString();
                            if (!finalAnswer.isBlank()) {
                                var processingContext = new RagProcessingContext(
                                        requestId, query, documentsRef.get(), promptRef.get(),
                                        new RagAnswer(finalAnswer, List.of()), sessionId);
                                postProcessingOrchestrator.process(processingContext);
                            }
                        })
                        .onErrorResume(e -> {
                            log.error("Ошибка в 'чистом' потоке RAG для запроса '{}': {}", query, e.getMessage());
                            return Flux.just(new StreamingResponsePart.Error("Произошла внутренняя ошибка."));
                        })
        );
    }

    /**
     * Внутренний метод, подготавливающий асинхронный конвейер до этапа генерации.
     *
     * @param query               Запрос пользователя.
     * @param history             История чата.
     * @param topK                Количество извлекаемых документов.
     * @param similarityThreshold Порог схожести.
     * @return {@link Mono}, который эммитит контекст, готовый для генерации.
     */
    private Mono<RagFlowContext> prepareRagFlow(String query, List<Message> history, int topK, double similarityThreshold) {
        return Mono.fromRunnable(() -> promptGuardService.checkForInjection(query))
                .then(Mono.defer(() ->
                        // ШАГ 1: Маршрутизация запроса для получения фильтра
                        Mono.fromFuture(() -> knowledgeRouterAgent.execute(new AgentContext(Map.of("query", query))))
                                .flatMap(routerResult -> {
                                    @SuppressWarnings("unchecked")
                                    List<String> domains = (List<String>) routerResult.details().get("selectedDomains");
                                    Filter.Expression domainFilter = buildDomainFilter(domains);

                                    // ШАГ 2: Обработка и извлечение с использованием фильтра
                                    return queryProcessingPipeline.process(query)
                                            .onErrorResume(e -> {
                                                log.warn("Ошибка в конвейере обработки запроса. Используется оригинальный запрос. Ошибка: {}", e.getMessage());
                                                return Mono.just(new ProcessedQueries(query, List.of()));
                                            })
                                            .flatMap(processedQueries -> retrievalStrategy.retrieve(processedQueries, query, topK, similarityThreshold, domainFilter))
                                            .map(fusedDocs -> rerankingService.rerank(fusedDocs, query))
                                            .flatMap(rerankedDocuments ->
                                                    augmentationService.augment(rerankedDocuments, query, history)
                                                            .map(prompt -> new RagFlowContext(rerankedDocuments, prompt))
                                            );
                                })
                ))
                .onErrorResume(e -> {
                    log.error("Критическая ошибка в RAG-конвейере для запроса '{}'", query, e);
                    return Mono.error(e);
                });
    }

    /**
     * Строит выражение фильтра на основе списка доменов.
     *
     * @param domains Список имен доменов.
     * @return {@link Filter.Expression} или null, если домены не выбраны.
     */
    private Filter.Expression buildDomainFilter(List<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return null;
        }
        return new Filter.Expression(Filter.ExpressionType.IN, new Filter.Key("metadata.doc_category"), new Filter.Value(domains));
    }

    /**
     * Внутренний DTO для передачи данных между этапами асинхронного потока.
     *
     * @param documents Список извлеченных и переранжированных документов.
     * @param prompt    Финальный промпт, готовый к отправке в LLM.
     */
    private record RagFlowContext(List<Document> documents, Prompt prompt) {
    }
}
