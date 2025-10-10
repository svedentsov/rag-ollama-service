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
 * Шаг RAG-конвейера для предварительной обработки запроса, адаптированный для R2DBC.
 */
@Component
@Order(10)
@Slf4j
@RequiredArgsConstructor
public class QueryProcessingStep implements RagPipelineStep {

    private final QueryProcessingPipeline queryProcessingPipeline;
    private final TaskLifecycleService taskLifecycleService;

    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [10] Query Processing: запуск обработки запроса '{}'", context.originalQuery());

        taskLifecycleService.getActiveTaskForSession(context.sessionId())
                .doOnNext(task -> taskLifecycleService.emitEvent(task.getId(), new UniversalResponse.StatusUpdate("Анализирую ваш вопрос...")))
                .subscribe();

        return queryProcessingPipeline.process(context.originalQuery())
                .map(context::withProcessedQueries);
    }
}
