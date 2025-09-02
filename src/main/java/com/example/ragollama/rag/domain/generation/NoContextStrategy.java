package com.example.ragollama.rag.domain.generation;

import com.example.ragollama.rag.domain.model.RagAnswer;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Mono;

/**
 * Стратегия, определяющая поведение RAG-системы, когда на этапе
 * Retrieval не было найдено ни одного релевантного документа.
 */
@FunctionalInterface
public interface NoContextStrategy {
    /**
     * Выполняет действие при отсутствии контекста для асинхронного "запрос-ответ" сценария.
     *
     * @param prompt Собранный промпт.
     * @return {@link Mono} с финальным {@link RagAnswer}.
     */
    Mono<RagAnswer> handle(Prompt prompt);
}
