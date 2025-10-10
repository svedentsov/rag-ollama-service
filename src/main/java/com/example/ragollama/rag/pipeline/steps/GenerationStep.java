package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.GenerationService;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Шаг RAG-конвейера, отвечающий за финальную генерацию ответа с использованием LLM.
 * <p>
 * Этот класс является адаптером, который связывает интерфейс {@link RagPipelineStep}
 * с конкретной реализацией в {@link GenerationService}. Он делегирует всю
 * сложную логику нижележащему сервису.
 */
@Component
@Order(40)
@RequiredArgsConstructor
@Slf4j
public class GenerationStep implements RagPipelineStep {

    private final GenerationService generationService;

    /**
     * {@inheritDoc}
     * <p>
     * Делегирует выполнение асинхронного "запрос-ответ" сценария сервису {@link GenerationService}.
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [40] Generation: делегирование генерации полного ответа...");
        return generationService.generate(context.finalPrompt(), context.rerankedDocuments())
                .map(context::withFinalAnswer);
    }

    /**
     * Делегирует выполнение потоковой генерации ответа сервису {@link GenerationService}.
     * <p>
     * Этот метод вызывается напрямую оркестратором для потоковых сценариев.
     *
     * @param prompt    Финальный промпт для LLM.
     * @param documents Документы, использованные в качестве контекста.
     * @return Реактивный поток {@link Flux} со структурированными частями ответа.
     */
    public Flux<StreamingResponsePart> generateStructuredStream(Prompt prompt, List<Document> documents) {
        log.info("Шаг [40] Generation (Stream): делегирование генерации потокового ответа...");
        return generationService.generateStructuredStream(prompt, documents);
    }
}
