package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
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
import java.util.stream.Collectors;

/**
 * Главный сервис-оркестратор, управляющий выполнением RAG-конвейера в реактивном стиле.
 * <p>
 * Этот класс является ядром RAG-системы. Он динамически собирает все шаги конвейера
 * (реализации {@link RagPipelineStep}), упорядочивает их и выполняет в правильной
 * последовательности, используя мощь Project Reactor для управления асинхронными операциями.
 * <p>
 * Оркестратор четко разделяет:
 * <ul>
 *     <li><b>Предварительную обработку:</b> Все шаги до генерации ответа LLM.</li>
 *     <li><b>Генерацию:</b> Непосредственно вызов LLM, инкапсулированный в {@link GenerationStep}.</li>
 *     <li><b>Постобработку:</b> Асинхронные задачи, выполняемые "в фоне" после отправки ответа
 *     (логирование, сбор метрик, верификация), управляемые через {@link RagPostProcessingOrchestrator}.</li>
 * </ul>
 */
@Slf4j
@Service
public class RagPipelineOrchestrator {

    private final RagPostProcessingOrchestrator postProcessingOrchestrator;
    private final List<RagPipelineStep> preGenerationSteps;
    private final GenerationStep generationStep;
    private final com.example.ragollama.monitoring.AuditLoggingService.AuditLoggingService auditLoggingService;

    /**
     * Конструктор, который автоматически внедряет все доступные шаги конвейера
     * и разделяет их на этапы до и во время генерации.
     *
     * @param allPipelineSteps           Список всех бинов, реализующих {@link RagPipelineStep}.
     * @param postProcessingOrchestrator Оркестратор для фоновых задач постобработки.
     * @param auditLoggingService        Сервис для записи аудиторских логов.
     */
    public RagPipelineOrchestrator(List<RagPipelineStep> allPipelineSteps,
                                   RagPostProcessingOrchestrator postProcessingOrchestrator,
                                   com.example.ragollama.monitoring.AuditLoggingService.AuditLoggingService auditLoggingService) {
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

    /**
     * Асинхронно выполняет полный RAG-конвейер для получения единого ответа.
     *
     * @param query               Исходный вопрос пользователя.
     * @param history             История диалога для поддержания контекста.
     * @param topK                Количество извлекаемых документов.
     * @param similarityThreshold Порог схожести для векторного поиска.
     * @param sessionId           Идентификатор сессии.
     * @return {@link Mono}, который по завершении будет содержать полный объект {@link RagAnswer}.
     */
    public Mono<RagAnswer> queryAsync(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        final String requestId = MDC.get("requestId");
        RagFlowContext initialContext = new RagFlowContext(query, history, topK, similarityThreshold, sessionId);

        return executePreGenerationPipeline(initialContext)
                .flatMap(generationStep::process)
                .map(finalContext -> {
                    auditLoggingService.logInteraction(
                            requestId,
                            finalContext.sessionId(),
                            finalContext.originalQuery(),
                            finalContext.finalAnswer().sourceCitations(),
                            finalContext.finalPrompt().getContents(),
                            finalContext.finalAnswer().answer()
                    );
                    var processingContext = new RagProcessingContext(
                            requestId, finalContext.originalQuery(), finalContext.rerankedDocuments(),
                            finalContext.finalPrompt(), finalContext.finalAnswer(), finalContext.sessionId());
                    postProcessingOrchestrator.process(processingContext);
                    return finalContext.finalAnswer();
                });
    }

    /**
     * Асинхронно выполняет полный RAG-конвейер в потоковом режиме (Server-Sent Events).
     *
     * @param query               Исходный вопрос пользователя.
     * @param history             История диалога.
     * @param topK                Количество извлекаемых документов.
     * @param similarityThreshold Порог схожести.
     * @param sessionId           Идентификатор сессии.
     * @return {@link Flux} со структурированными частями ответа {@link StreamingResponsePart}.
     */
    public Flux<StreamingResponsePart> queryStream(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        final String requestId = MDC.get("requestId");
        RagFlowContext initialContext = new RagFlowContext(query, history, topK, similarityThreshold, sessionId);

        return executePreGenerationPipeline(initialContext).flatMapMany(context -> {
            Flux<StreamingResponsePart> stream = generationStep.generateStructuredStream(context.finalPrompt(), context.rerankedDocuments());

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
                auditLoggingService.logInteraction(
                        requestId, sessionId, query, citations,
                        context.finalPrompt().getContents(), fullAnswer
                );
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
