package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.optimization.ReflectiveRetrieverAgent;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class RetrievalStep implements RagPipelineStep {

    private final ReflectiveRetrieverAgent reflectiveRetrieverAgent;

    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [20] Retrieval: запуск рефлексивного поиска...");
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
