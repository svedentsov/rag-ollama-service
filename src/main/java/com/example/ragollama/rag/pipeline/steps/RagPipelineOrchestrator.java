package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.monitoring.AuditLoggingService.AuditLoggingService;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.rag.postprocessing.RagPostProcessingOrchestrator;
import com.example.ragollama.rag.postprocessing.RagProcessingContext;
import com.example.ragollama.shared.task.TaskLifecycleService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Главный сервис-оркестратор, управляющий выполнением RAG-конвейера в реактивном стиле.
 */
@Slf4j
@Service
public class RagPipelineOrchestrator {

    private final RagPostProcessingOrchestrator postProcessingOrchestrator;
    private final List<RagPipelineStep> preGenerationSteps;
    private final GenerationStep generationStep;
    private final AuditLoggingService auditLoggingService;
    private final TaskLifecycleService taskLifecycleService;

    public RagPipelineOrchestrator(List<RagPipelineStep> allPipelineSteps,
                                   RagPostProcessingOrchestrator postProcessingOrchestrator,
                                   AuditLoggingService auditLoggingService,
                                   TaskLifecycleService taskLifecycleService) {
        this.postProcessingOrchestrator = postProcessingOrchestrator;
        this.auditLoggingService = auditLoggingService;
        this.taskLifecycleService = taskLifecycleService;

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

    public Mono<RagAnswer> queryAsync(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId, UUID taskId) {
        final String requestId = MDC.get("requestId");
        RagFlowContext initialContext = new RagFlowContext(query, history, topK, similarityThreshold, sessionId);

        return executePreGenerationPipeline(initialContext)
                .flatMap(generationStep::process)
                .doOnNext(finalContext -> {
                    auditLoggingService.logInteraction(
                            requestId,
                            taskId,
                            finalContext.sessionId(),
                            finalContext.originalQuery(),
                            finalContext.finalAnswer().sourceCitations(),
                            finalContext.finalPrompt().getContents(),
                            finalContext.finalAnswer().answer(),
                            finalContext.finalAnswer().queryFormationHistory()
                    ).subscribe();

                    var processingContext = new RagProcessingContext(
                            requestId, finalContext.originalQuery(), finalContext.rerankedDocuments(),
                            finalContext.finalPrompt(), finalContext.finalAnswer(), finalContext.sessionId());
                    postProcessingOrchestrator.process(processingContext);
                })
                .map(RagFlowContext::finalAnswer);
    }

    public Flux<StreamingResponsePart> queryStream(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId, UUID taskId) {
        final String requestId = MDC.get("requestId");
        RagFlowContext initialContext = new RagFlowContext(query, history, topK, similarityThreshold, sessionId);

        return executePreGenerationPipeline(initialContext).flatMapMany(context -> {
            Flux<StreamingResponsePart> stream = generationStep.generateStructuredStream(
                    context.finalPrompt(),
                    context.rerankedDocuments(),
                    context.processedQueries() != null ? context.processedQueries().formationHistory() : List.of()
            );

            // Собираем полный ответ в фоне для логирования и постобработки
            stream.collectList().subscribe(parts -> {
                String fullAnswer = parts.stream()
                        .filter(p -> p instanceof StreamingResponsePart.Content)
                        .map(p -> ((StreamingResponsePart.Content) p).text())
                        .collect(Collectors.joining());

                List<SourceCitation> citations = parts.stream()
                        .filter(p -> p instanceof StreamingResponsePart.Sources)
                        .flatMap(p -> ((StreamingResponsePart.Sources) p).sources().stream())
                        .toList();

                RagAnswer answerForPostProcessing = new RagAnswer(fullAnswer, citations, context.processedQueries() != null ? context.processedQueries().formationHistory() : List.of(), context.finalPrompt().getContents());

                auditLoggingService.logInteraction(
                        requestId, taskId, sessionId, query, citations,
                        context.finalPrompt().getContents(), fullAnswer, answerForPostProcessing.queryFormationHistory()
                ).subscribe();

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
