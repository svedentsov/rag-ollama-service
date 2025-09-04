package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Шаг RAG-конвейера, отвечающий за извлечение (Retrieval) документов.
 */
@Component
@Order(20) // Выполняется после обработки запроса
@RequiredArgsConstructor
@Slf4j
public class RetrievalStep implements RagPipelineStep {

    private final HybridRetrievalStrategy retrievalStrategy;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [20] Retrieval: запуск гибридного поиска...");
        return retrievalStrategy.retrieve(
                        context.processedQueries(),
                        context.originalQuery(),
                        context.topK(),
                        context.similarityThreshold(),
                        null
                )
                .map(context::withRetrievedDocuments);
    }
}
