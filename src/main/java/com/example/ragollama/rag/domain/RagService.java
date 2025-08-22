package com.example.ragollama.rag.domain;

import com.example.ragollama.monitoring.GroundingService;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
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
import java.util.concurrent.CompletableFuture;

/**
 * "Чистый" сервис-оркестратор RAG-конвейера.
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
    private final GroundingService groundingService;

    public record RagAnswer(String answer, List<String> sourceCitations) {
    }

    public CompletableFuture<RagAnswer> queryAsync(String query, List<Message> history, int topK, double similarityThreshold) {
        return metricService.recordTimer("rag.requests.async.pure",
                () -> prepareRagFlow(query, history, topK, similarityThreshold)
                        .flatMap(context -> {
                            String promptContent = context.prompt().getContents();
                            return Mono.fromFuture(generationService.generate(context.prompt(), context.documents(), null))
                                    .doOnSuccess(response -> {
                                        if (Math.random() < 0.1) {
                                            groundingService.verify(promptContent, response.answer());
                                        }
                                    });
                        })
                        .map(response -> new RagAnswer(response.answer(), response.sourceCitations()))
                        .toFuture()
        );
    }

    public Flux<StreamingResponsePart> queryStream(String query, List<Message> history, int topK, double similarityThreshold) {
        return metricService.recordTimer("rag.requests.stream.pure",
                () -> prepareRagFlow(query, history, topK, similarityThreshold)
                        .flatMapMany(context -> generationService.generateStructuredStream(context.prompt(), context.documents(), null))
                        .onErrorResume(e -> {
                            log.error("Ошибка в 'чистом' потоке RAG для запроса '{}': {}", query, e.getMessage());
                            return Flux.just(new StreamingResponsePart.Error("Произошла внутренняя ошибка."));
                        })
        );
    }

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

    private record RagFlowContext(List<Document> documents, Prompt prompt) {
    }
}
