package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.optimization.ReflectiveRetrieverAgent;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.task.TaskLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Шаг RAG-конвейера для извлечения документов, адаптированный для R2DBC.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class RetrievalStep implements RagPipelineStep {

    private final ReflectiveRetrieverAgent reflectiveRetrieverAgent;
    private final TaskLifecycleService taskLifecycleService;

    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [20] Retrieval: запуск рефлексивного поиска...");

        taskLifecycleService.getActiveTaskForSession(context.sessionId())
                .doOnNext(task -> taskLifecycleService.emitEvent(task.getId(), new UniversalResponse.StatusUpdate("Ищу информацию в базе знаний...")))
                .subscribe();

        return reflectiveRetrieverAgent.retrieve(
                        context.processedQueries(),
                        context.originalQuery(),
                        context.topK(),
                        context.similarityThreshold(),
                        null
                )
                .map(context::withRetrievedDocuments);
    }
}
