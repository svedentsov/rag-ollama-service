package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.optimization.QueryProfilerAgent;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Шаг RAG-конвейера, отвечающий за предварительную обработку и расширение
 * исходного запроса пользователя.
 * <p> Этот шаг является "мозгом" на входе в RAG. Он использует AI-агентов для
 * трансформации и генерации нескольких вариантов запроса, что критически
 * важно для повышения полноты (Recall) и точности (Precision) поиска.
 *
 * @see QueryProcessingPipeline
 * @see QueryProfilerAgent
 */
@Component
@Order(10)
@Slf4j
@RequiredArgsConstructor
public class QueryProcessingStep implements RagPipelineStep {

    private final QueryProcessingPipeline queryProcessingPipeline;

    /**
     * {@inheritDoc}
     *
     * @param context Текущий контекст выполнения, содержащий исходный запрос.
     * @return {@link Mono} с обновленным контекстом, обогащенным обработанными запросами.
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [10] Query Processing: запуск обработки запроса '{}'", context.originalQuery());
        return queryProcessingPipeline.process(context.originalQuery())
                .map(context::withProcessedQueries);
    }
}
