package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.domain.generation.NoContextStrategy;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.exception.GenerationException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Шаг RAG-конвейера, отвечающий за вызов LLM для генерации "сырого" ответа.
 * <p>
 * Этот шаг не занимается обработкой цитат. Его единственная задача — вызвать LLM
 * с подготовленным промптом и передать "сырой" ответ (с inline-цитатами)
 * на следующий этап для парсинга.
 */
@Component
@Order(40) // Выполняется после Augmentation
@RequiredArgsConstructor
@Slf4j
public class GenerationStep implements RagPipelineStep {

    private final LlmClient llmClient;
    private final NoContextStrategy noContextStrategy;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [40] Generation: вызов LLM...");
        // Если на предыдущих шагах не нашлось документов, применяем соответствующую стратегию.
        if (context.rerankedDocuments().isEmpty()) {
            log.warn("На этап Generation не передано документов. Применяется стратегия '{}'.", noContextStrategy.getClass().getSimpleName());
            return noContextStrategy.handle(context.finalPrompt())
                    .map(context::withFinalAnswer);
        }

        return Mono.fromFuture(llmClient.callChat(context.finalPrompt(), ModelCapability.BALANCED))
                .map(rawAnswer -> context.withFinalAnswer(new com.example.ragollama.rag.domain.model.RagAnswer(rawAnswer, null))) // Цитаты будут извлечены на следующем шаге
                .doOnError(ex -> log.error("Ошибка на этапе генерации ответа LLM", ex))
                .onErrorMap(ex -> new GenerationException("Не удалось сгенерировать ответ от LLM.", ex));
    }
}
