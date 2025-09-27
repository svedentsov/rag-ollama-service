package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.task.CancellableTaskService;
import com.example.ragollama.shared.task.TaskStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Шаг RAG-конвейера, отвечающий за предварительную обработку и расширение
 * исходного запроса пользователя.
 * <p>
 * Этот шаг является "мозгом" на входе в RAG. Он использует AI-агентов для
 * трансформации и генерации нескольких вариантов запроса, что критически
 * важно для повышения полноты (Recall) и точности (Precision) поиска.
 * <p>
 * Также этот шаг отвечает за отправку первого статусного сообщения в UI,
 * информируя пользователя о начале анализа его запроса.
 *
 * @see QueryProcessingPipeline
 */
@Component
@Order(10) // Выполняется после PromptGuard (1)
@Slf4j
@RequiredArgsConstructor
public class QueryProcessingStep implements RagPipelineStep {

    private final QueryProcessingPipeline queryProcessingPipeline;
    private final TaskStateService taskStateService;
    private final CancellableTaskService taskService;

    /**
     * {@inheritDoc}
     * <p>
     * Асинхронно запускает конвейер обработки запроса и обогащает контекст
     * его результатами. Перед началом обработки отправляет статусное событие
     * на фронтенд.
     *
     * @param context Текущий контекст выполнения, содержащий исходный запрос.
     * @return {@link Mono} с обновленным контекстом, обогащенным обработанными запросами.
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [10] Query Processing: запуск обработки запроса '{}'", context.originalQuery());
        taskStateService.getActiveTaskIdForSession(context.sessionId()).ifPresent(taskId ->
                taskService.emitEvent(taskId, new UniversalResponse.StatusUpdate("Анализирую ваш вопрос...")));
        return queryProcessingPipeline.process(context.originalQuery())
                .map(context::withProcessedQueries);
    }
}