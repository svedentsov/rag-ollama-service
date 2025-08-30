package com.example.ragollama.rag.domain;

import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.postprocessing.RagPostProcessingOrchestrator;
import com.example.ragollama.rag.postprocessing.RagProcessingContext;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
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
 * "Чистый" сервис-оркестратор RAG-конвейера, освобожденный от сквозных задач.
 * <p>
 * Эта финальная, отрефакторенная версия делегирует всю логику постобработки
 * (аудит, верификация, метрики) специализированному сервису
 * {@link RagPostProcessingOrchestrator}. Это делает `RagService` сфокусированным,
 * легко тестируемым и соответствующим Принципу Единственной Ответственности (SRP).
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
    private final RagPostProcessingOrchestrator postProcessingOrchestrator;

    /**
     * DTO для инкапсуляции результата работы RAG-сервиса.
     *
     * @param answer          Сгенерированный ответ.
     * @param sourceCitations Список источников.
     */
    public record RagAnswer(String answer, List<String> sourceCitations) {
    }

    /**
     * Асинхронно выполняет полный RAG-запрос (не-потоковый).
     *
     * @param query               Запрос пользователя.
     * @param history             История чата для контекста.
     * @param topK                Количество извлекаемых документов.
     * @param similarityThreshold Порог схожести.
     * @param sessionId           Идентификатор сессии для аудита.
     * @return {@link CompletableFuture} с финальным ответом.
     */
    public CompletableFuture<RagAnswer> queryAsync(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        return metricService.recordTimer("rag.requests.async.pure",
                () -> prepareRagFlow(query, history, topK, similarityThreshold)
                        .flatMap(context ->
                                Mono.fromFuture(generationService.generate(context.prompt(), context.documents(), sessionId))
                                        .doOnSuccess(response -> {
                                            var processingContext = new RagProcessingContext(query, context.documents(), context.prompt(),
                                                    new RagAnswer(response.answer(), response.sourceCitations()), sessionId);
                                            postProcessingOrchestrator.process(processingContext);
                                        })
                        )
                        .map(response -> new RagAnswer(response.answer(), response.sourceCitations()))
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
        return metricService.recordTimer("rag.requests.stream.pure",
                () -> prepareRagFlow(query, history, topK, similarityThreshold)
                        .flatMapMany(context -> generationService.generateStructuredStream(context.prompt(), context.documents(), sessionId))
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
                        queryProcessingPipeline.process(query)
                                .flatMap(processedQueries -> retrievalStrategy.retrieve(processedQueries, query, topK, similarityThreshold, null))
                                .flatMap(rerankedDocuments ->
                                        augmentationService.augment(rerankedDocuments, query, history)
                                                .map(prompt -> new RagFlowContext(rerankedDocuments, prompt))
                                )
                ))
                .onErrorResume(e -> {
                    log.error("Критическая ошибка в 'чистом' RAG-конвейере для запроса '{}'", query, e);
                    return Mono.error(e);
                });
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
