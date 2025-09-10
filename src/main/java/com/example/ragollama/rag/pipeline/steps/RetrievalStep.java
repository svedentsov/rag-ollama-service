package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.optimization.ReflectiveRetrieverAgent;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Шаг RAG-конвейера, отвечающий за извлечение релевантных документов.
 * <p>Эта версия делегирует всю сложную логику поиска новому
 * {@link ReflectiveRetrieverAgent}, который реализует итеративный, самокорректирующийся цикл поиска.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class RetrievalStep implements RagPipelineStep {

    private final ReflectiveRetrieverAgent reflectiveRetrieverAgent;

    /**
     * {@inheritDoc}
     *
     * @param context Текущий контекст RAG-конвейера.
     * @return {@link Mono} с обновленным контекстом, содержащим извлеченные документы.
     */
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
