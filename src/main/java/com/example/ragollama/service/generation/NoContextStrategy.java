package com.example.ragollama.service.generation;

import com.example.ragollama.dto.RagQueryResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Стратегия, определяющая поведение RAG-системы, когда на этапе
 * Retrieval не было найдено ни одного релевантного документа.
 */
public interface NoContextStrategy {
    /**
     * Выполняет действие при отсутствии контекста для асинхронного "запрос-ответ" сценария.
     *
     * @param prompt    Собранный промпт.
     * @param sessionId Идентификатор текущей сессии чата.
     * @return {@link Mono} с финальным {@link RagQueryResponse}.
     */
    Mono<RagQueryResponse> handle(Prompt prompt, UUID sessionId);
}
