package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.optimization.GraphContextExpanderService;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.task.TaskLifecycleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Шаг RAG-конвейера для расширения контекста через Граф Знаний, адаптированный для R2DBC.
 */
@Component
@Order(27)
@Slf4j
@ConditionalOnProperty(name = "app.expansion.graph.enabled", havingValue = "true")
public class GraphExpansionStep implements RagPipelineStep {

    private final GraphContextExpanderService expanderService;
    private final TaskLifecycleService taskLifecycleService;

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param expanderService      Сервис для расширения контекста.
     * @param appProperties        Конфигурация приложения.
     * @param taskLifecycleService Сервис для управления задачами.
     */
    public GraphExpansionStep(GraphContextExpanderService expanderService, AppProperties appProperties, TaskLifecycleService taskLifecycleService) {
        this.expanderService = expanderService;
        this.taskLifecycleService = taskLifecycleService;
        if (appProperties.expansion().graph().enabled()) {
            log.info("Активирован шаг конвейера: GraphExpansionStep");
        }
    }

    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        if (context.rerankedDocuments().isEmpty()) {
            return Mono.just(context);
        }
        log.info("Шаг [27] Graph Expansion: расширение контекста через Граф Знаний...");

        taskLifecycleService.getActiveTaskForSession(context.sessionId())
                .doOnNext(task -> taskLifecycleService.emitEvent(task.getId(), new UniversalResponse.StatusUpdate("Ищу связи в графе знаний...")))
                .subscribe();

        return expanderService.expand(context.rerankedDocuments())
                .map(context::withRerankedDocuments);
    }
}
