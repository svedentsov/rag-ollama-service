package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.task.TaskLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Шаг RAG-конвейера для предварительной обработки и улучшения запроса пользователя.
 * <p>
 * Этот шаг делегирует выполнение цепочки агентов по улучшению запроса
 * (HyDE, Multi-Query, и т.д.) специализированному сервису {@link QueryProcessingPipeline}.
 * Также информирует клиента о текущем статусе.
 */
@Component
@Order(10)
@Slf4j
@RequiredArgsConstructor
public class QueryProcessingStep implements RagPipelineStep {

    private final QueryProcessingPipeline queryProcessingPipeline;
    private final TaskLifecycleService taskLifecycleService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [10] Query Processing: запуск обработки запроса '{}'", context.originalQuery());

        // Асинхронно отправляем событие статуса клиенту, не блокируя основной поток
        taskLifecycleService.getActiveTaskForSession(context.sessionId())
                .doOnNext(task -> taskLifecycleService.emitEvent(task.getId(), new UniversalResponse.StatusUpdate("Анализирую ваш вопрос...")))
                .subscribe();

        return queryProcessingPipeline.process(context.originalQuery())
                .map(context::withProcessedQueries);
    }
}
