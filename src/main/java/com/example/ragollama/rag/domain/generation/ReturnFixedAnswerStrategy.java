package com.example.ragollama.rag.domain.generation;

import com.example.ragollama.rag.api.dto.RagQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

/**
 * Реализация {@link NoContextStrategy}, которая возвращает заранее
 * определенный, статический ответ без обращения к LLM.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.rag.no-context-strategy", havingValue = "fixed", matchIfMissing = true)
public class ReturnFixedAnswerStrategy implements NoContextStrategy {

    private static final String NO_CONTEXT_ANSWER = "Извините, я не смог найти релевантную информацию в базе знаний по вашему вопросу.";

    public ReturnFixedAnswerStrategy() {
        log.info("Активирована стратегия NoContextStrategy: ReturnFixedAnswerStrategy");
    }

    /**
     * Возвращает стандартный ответ-заглушку, включая ID сессии.
     *
     * @param prompt    Промпт (игнорируется).
     * @param sessionId Идентификатор текущей сессии.
     * @return {@link Mono}, немедленно завершающийся с {@link RagQueryResponse}.
     */
    @Override
    public Mono<RagQueryResponse> handle(Prompt prompt, UUID sessionId) {
        return Mono.just(new RagQueryResponse(NO_CONTEXT_ANSWER, Collections.emptyList(), sessionId));
    }
}
