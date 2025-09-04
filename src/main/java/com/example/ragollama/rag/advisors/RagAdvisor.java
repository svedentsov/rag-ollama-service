package com.example.ragollama.rag.advisors;

import com.example.ragollama.rag.pipeline.RagFlowContext;
import reactor.core.publisher.Mono;

/**
 * Асинхронный функциональный интерфейс для реализации паттерна "Советник" (Advisor).
 * <p>
 * Каждый советник представляет собой отдельный, независимый и переиспользуемый
 * компонент бизнес-логики. В этой версии интерфейс полностью асинхронный
 * и работает с унифицированным контекстным объектом {@link RagFlowContext}.
 */
@FunctionalInterface
public interface RagAdvisor {
    /**
     * Асинхронно применяет свою логику к контексту RAG-запроса.
     *
     * @param context Текущий контекст запроса, содержащий все промежуточные данные конвейера.
     * @return {@link Mono}, который по завершении будет содержать
     * модифицированный контекст для следующего советника в цепочке.
     */
    Mono<RagFlowContext> advise(RagFlowContext context);
}
