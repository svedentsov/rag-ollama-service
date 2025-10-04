package com.example.ragollama.rag.pipeline;

import com.example.ragollama.monitoring.AuditLoggingService.AuditLoggingService;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.pipeline.steps.GenerationStep;
import com.example.ragollama.rag.postprocessing.RagPostProcessingOrchestrator;
import com.example.ragollama.rag.postprocessing.RagProcessingContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Оркестратор RAG-конвейера.
 */
@Slf4j
@Service
public class RagPipelineOrchestrator {

    private final RagPostProcessingOrchestrator postProcessingOrchestrator;
    private final List<RagPipelineStep> preGenerationSteps;
    private final GenerationStep generationStep;
    private final AuditLoggingService auditLoggingService;

    public RagPipelineOrchestrator(List<RagPipelineStep> allPipelineSteps,
                                   RagPostProcessingOrchestrator postProcessingOrchestrator,
                                   AuditLoggingService auditLoggingService) {
        this.postProcessingOrchestrator = postProcessingOrchestrator;
        this.auditLoggingService = auditLoggingService;
        this.generationStep = allPipelineSteps.stream()
                .filter(GenerationStep.class::isInstance)
                .map(GenerationStep.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Конфигурация невалидна: в конвейере должен быть ровно один GenerationStep."));
        this.preGenerationSteps = allPipelineSteps.stream()
                .filter(step -> !(step instanceof GenerationStep))
                .toList();
        log.info("RagPipelineOrchestrator инициализирован. Пред-генерационных шагов: {}, Шаг генерации: {}.",
                preGenerationSteps.size(), generationStep.getClass().getSimpleName());
    }

    public CompletableFuture<RagAnswer> queryAsync(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        final String requestId = MDC.get("requestId");
        RagFlowContext initialContext = new RagFlowContext(query, history, topK, similarityThreshold, sessionId);

        return executePreGenerationPipeline(initialContext)
                .flatMap(generationStep::process)
                .map(finalContext -> {
                    // Синхронное логирование ДО возврата ответа
                    auditLoggingService.logInteraction(
                            requestId,
                            finalContext.sessionId(),
                            finalContext.originalQuery(),
                            finalContext.finalAnswer().sourceCitations(),
                            finalContext.finalPrompt().getContents(),
                            finalContext.finalAnswer().answer()
                    );
                    // Асинхронная постобработка
                    var processingContext = new RagProcessingContext(
                            requestId, finalContext.originalQuery(), finalContext.rerankedDocuments(),
                            finalContext.finalPrompt(), finalContext.finalAnswer(), finalContext.sessionId());
                    postProcessingOrchestrator.process(processingContext);
                    return finalContext.finalAnswer();
                })
                .toFuture();
    }

    public Flux<StreamingResponsePart> queryStream(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        final String requestId = MDC.get("requestId");
        RagFlowContext initialContext = new RagFlowContext(query, history, topK, similarityThreshold, sessionId);

        return executePreGenerationPipeline(initialContext).flatMapMany(context -> {
            Flux<StreamingResponsePart> stream = generationStep.generateStructuredStream(context.finalPrompt(), context.rerankedDocuments());
            // Собираем полный ответ в фоне для постобработки и аудита
            stream.collectList().subscribe(parts -> {
                String fullAnswer = parts.stream()
                        .filter(p -> p instanceof StreamingResponsePart.Content)
                        .map(p -> ((StreamingResponsePart.Content) p).text())
                        .collect(Collectors.joining());
                List<com.example.ragollama.rag.domain.model.SourceCitation> citations = parts.stream()
                        .filter(p -> p instanceof StreamingResponsePart.Sources)
                        .flatMap(p -> ((StreamingResponsePart.Sources) p).sources().stream())
                        .toList();
                RagAnswer answerForPostProcessing = new RagAnswer(fullAnswer, citations);
                // Синхронное логирование после завершения стрима
                auditLoggingService.logInteraction(
                        requestId, sessionId, query, citations,
                        context.finalPrompt().getContents(), fullAnswer
                );
                // Асинхронная постобработка
                var processingContext = new RagProcessingContext(requestId, query, context.rerankedDocuments(),
                        context.finalPrompt(), answerForPostProcessing, sessionId);
                postProcessingOrchestrator.process(processingContext);
            });
            return stream;
        });
    }

    private Mono<RagFlowContext> executePreGenerationPipeline(RagFlowContext initialContext) {
        return Flux.fromIterable(preGenerationSteps)
                .reduce(Mono.just(initialContext), (contextMono, step) -> contextMono.flatMap(step::process))
                .flatMap(mono -> mono);
    }
}
