package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.optimization.ReflectiveRetrieverAgent;
import com.example.ragollama.orchestration.dto.UniversalResponse;
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
 * Шаг RAG-конвейера, отвечающий за извлечение релевантных документов.
 * <p>Эта версия делегирует всю сложную логику поиска новому
 * {@link ReflectiveRetrieverAgent}, который реализует итеративный, самокорректирующийся цикл поиска,
 * и отправляет обновление статуса в UI.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class RetrievalStep implements RagPipelineStep {

    private final ReflectiveRetrieverAgent reflectiveRetrieverAgent;
    private final TaskStateService taskStateService;
    private final CancellableTaskService taskService;

    /**
     * {@inheritDoc}
     *
     * @param context Текущий контекст RAG-конвейера.
     * @return {@link Mono} с обновленным контекстом, содержащим извлеченные документы.
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [20] Retrieval: запуск рефлексивного поиска...");
        taskStateService.getActiveTaskIdForSession(context.sessionId()).ifPresent(taskId -> {
            taskService.emitEvent(taskId, new UniversalResponse.StatusUpdate("Ищу информацию в базе знаний..."));
        });
        return reflectiveRetrieverAgent.retrieve(
                        context.processedQueries(),
                        context.originalQuery(),
                        context.topK(),
                        context.similarityThreshold(),
                        null // бизнес-фильтр пока не используется
                )
                .map(context::withRetrievedDocuments);
    }
}
