package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.optimization.GraphContextExpanderService;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.config.properties.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Order(27)
@Slf4j
@ConditionalOnProperty(name = "app.expansion.graph.enabled", havingValue = "true")
public class GraphExpansionStep implements RagPipelineStep {

    private final GraphContextExpanderService expanderService;

    public GraphExpansionStep(GraphContextExpanderService expanderService, AppProperties appProperties) {
        this.expanderService = expanderService;
        log.info("Активирован шаг конвейера: GraphExpansionStep");
    }

    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [27] Graph Expansion: расширение контекста через Граф Знаний...");
        return expanderService.expand(context.rerankedDocuments())
                .map(context::withRerankedDocuments);
    }
}
