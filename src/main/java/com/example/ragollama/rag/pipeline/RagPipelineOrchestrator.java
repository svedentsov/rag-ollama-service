package com.example.ragollama.rag.pipeline;

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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Оркестратор RAG-конвейера, построенного на паттерне "Pipeline Step".
 * <p>
 * Этот класс является заменой для монолитного {@code RagService}. Его единственная
 * ответственность — получить от Spring отсортированный список всех шагов
 * (бинов, реализующих {@link RagPipelineStep}) и последовательно выполнить их,
 * передавая между ними объект состояния {@link RagFlowContext}.
 * <p>
 * Такая архитектура полностью соответствует Принципу открытости/закрытости: для
 * добавления нового шага в конвейер достаточно создать новый класс-реализацию
 * {@link RagPipelineStep} с аннотацией {@code @Order}, не изменяя код этого оркестратора.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPipelineOrchestrator {

    /**
     * Список всех шагов конвейера. Spring автоматически внедрит сюда все бины,
     * реализующие {@link RagPipelineStep}, и отсортирует их на основе
     * аннотации {@code @Order}.
     */
    private final List<RagPipelineStep> pipelineSteps;

    /**
     * Оркестратор для выполнения асинхронных задач постобработки (логирование, метрики и т.д.).
     */
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
        Mono<RagFlowContext> finalContextMono = Flux.fromIterable(pipelineSteps)
                .reduce(Mono.just(initialContext), (contextMono, step) -> contextMono.flatMap(step::process))
                .flatMap(mono -> mono);
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
}
