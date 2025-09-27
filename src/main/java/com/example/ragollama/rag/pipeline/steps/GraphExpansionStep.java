package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.optimization.GraphContextExpanderService;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.task.CancellableTaskService;
import com.example.ragollama.shared.task.TaskStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Шаг RAG-конвейера, который расширяет контекст, добавляя связанные сущности
 * из Графа Знаний.
 * <p>
 * Этот шаг активируется только если включен в конфигурации
 * (`app.expansion.graph.enabled=true`). Он использует {@link GraphContextExpanderService}
 * для выполнения Cypher-запросов и обогащения списка найденных документов.
 * <p>
 * Перед началом выполнения отправляет статусное сообщение в UI.
 */
@Component
@Order(27) // Выполняется после Reranking (25)
@Slf4j
@ConditionalOnProperty(name = "app.expansion.graph.enabled", havingValue = "true")
public class GraphExpansionStep implements RagPipelineStep {

    private final GraphContextExpanderService expanderService;
    private final TaskStateService taskStateService;
    private final CancellableTaskService taskService;

    public GraphExpansionStep(GraphContextExpanderService expanderService, AppProperties appProperties, TaskStateService taskStateService, CancellableTaskService taskService) {
        this.expanderService = expanderService;
        this.taskStateService = taskStateService;
        this.taskService = taskService;
        log.info("Активирован шаг конвейера: GraphExpansionStep");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        if (context.rerankedDocuments().isEmpty()) {
            return Mono.just(context);
        }
        log.info("Шаг [27] Graph Expansion: расширение контекста через Граф Знаний...");
        taskStateService.getActiveTaskIdForSession(context.sessionId()).ifPresent(taskId ->
                taskService.emitEvent(taskId, new UniversalResponse.StatusUpdate("Ищу связи в графе знаний...")));
        return expanderService.expand(context.rerankedDocuments())
                .map(context::withRerankedDocuments);
    }
}
