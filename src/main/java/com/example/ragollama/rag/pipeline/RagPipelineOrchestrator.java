package com.example.ragollama.rag.pipeline;

import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.postprocessing.RagPostProcessingOrchestrator;
import com.example.ragollama.rag.postprocessing.RagProcessingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Оркестратор RAG-конвейера, построенного на паттерне "Pipeline Step".
 * <p> Эта версия включает новый метод {@code queryStream} для поддержки сквозной
 * потоковой передачи данных.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPipelineOrchestrator {

    private final List<RagPipelineStep> pipelineSteps;
    private final RagPostProcessingOrchestrator postProcessingOrchestrator;

    /**
     * Асинхронно выполняет полный RAG-конвейер для не-потокового запроса.
     *
     * @param query               Исходный запрос пользователя.
     * @param history             История чата для поддержания контекста.
     * @param topK                Количество извлекаемых документов.
     * @param similarityThreshold Порог схожести для векторного поиска.
     * @param sessionId           Уникальный идентификатор текущей сессии.
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * финальный, готовый для пользователя {@link RagAnswer}.
     */
    public CompletableFuture<RagAnswer> queryAsync(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        final String requestId = MDC.get("requestId");
        RagFlowContext initialContext = new RagFlowContext(query, history, topK, similarityThreshold, sessionId);
        Mono<RagFlowContext> finalContextMono = executePipeline(initialContext);
        return finalContextMono
                .map(finalContext -> {
                    var processingContext = new RagProcessingContext(
                            requestId,
                            finalContext.originalQuery(),
                            finalContext.rerankedDocuments(),
                            finalContext.finalPrompt(),
                            finalContext.finalAnswer(),
                            finalContext.sessionId()
                    );
                    postProcessingOrchestrator.process(processingContext);
                    return finalContext.finalAnswer();
                })
                .toFuture();
    }

    /**
     * Выполняет RAG-конвейер в потоковом режиме.
     *
     * @param query               Запрос пользователя.
     * @param history             История чата.
     * @param topK                Количество документов для извлечения.
     * @param similarityThreshold Порог схожести.
     * @param sessionId           ID сессии.
     * @return {@link Flux} со структурированными частями ответа.
     */
    public Flux<StreamingResponsePart> queryStream(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        final String requestId = MDC.get("requestId");
        RagFlowContext initialContext = new RagFlowContext(query, history, topK, similarityThreshold, sessionId);
        Mono<RagFlowContext> contextReadyForGeneration = executePipeline(initialContext);
        return contextReadyForGeneration.flatMapMany(finalContext -> {
            RagAnswer answer = finalContext.finalAnswer();
            var processingContext = new RagProcessingContext(requestId, query, finalContext.rerankedDocuments(),
                    finalContext.finalPrompt(), answer, sessionId);
            postProcessingOrchestrator.process(processingContext);
            List<StreamingResponsePart> parts = new ArrayList<>();
            parts.add(new StreamingResponsePart.Content(answer.answer()));
            parts.add(new StreamingResponsePart.Sources(answer.sourceCitations()));
            parts.add(new StreamingResponsePart.Done("Stream completed"));
            return Flux.fromIterable(parts);
        });
    }

    /**
     * Вспомогательный метод для выполнения цепочки шагов конвейера.
     *
     * @param initialContext Начальный контекст.
     * @return {@link Mono} с финальным контекстом после выполнения всех шагов.
     */
    private Mono<RagFlowContext> executePipeline(RagFlowContext initialContext) {
        return Flux.fromIterable(pipelineSteps)
                .reduce(Mono.just(initialContext), (contextMono, step) -> contextMono.flatMap(step::process))
                .flatMap(mono -> mono);
    }
}
