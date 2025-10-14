package com.example.ragollama.rag.domain.generation;

import com.example.ragollama.rag.domain.model.RagAnswer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Реализация {@link NoContextStrategy}, которая возвращает заранее
 * определенный, статический ответ без обращения к LLM.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.rag.no-context-strategy", havingValue = "fixed", matchIfMissing = true)
public class ReturnFixedAnswerStrategy implements NoContextStrategy {

    private static final String NO_CONTEXT_ANSWER = "Извините, я не смог найти релевантную информацию в базе знаний по вашему вопросу.";

    /**
     * Конструктор, логирующий активацию данной стратегии при старте приложения.
     */
    public ReturnFixedAnswerStrategy() {
        log.info("Активирована стратегия NoContextStrategy: ReturnFixedAnswerStrategy");
    }

    /**
     * Возвращает стандартный ответ-заглушку.
     *
     * @param prompt Промпт (игнорируется).
     * @return {@link Mono}, немедленно завершающийся с {@link RagAnswer}.
     */
    @Override
    public Mono<RagAnswer> handle(Prompt prompt) {
        return Mono.just(new RagAnswer(NO_CONTEXT_ANSWER, Collections.emptyList(), Collections.emptyList(), null));
    }
}
