package com.example.ragollama.rag.advisors;

import com.example.ragollama.rag.pipeline.RagFlowContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Простой советник, который добавляет текущую дату в модель промпта.
 * <p>
 * Реализует асинхронный интерфейс {@link RagAdvisor} и работает с
 * унифицированным контекстом {@link RagFlowContext}.
 */
@Component
@Order(10) // Задаем порядок выполнения
public class CurrentDateAdvisor implements RagAdvisor {

    /**
     * Добавляет текущую дату в модель промпта, находящуюся в контексте.
     *
     * @param context Текущий контекст RAG-конвейера.
     * @return {@link Mono}, немедленно завершающийся с обновленным контекстом.
     */
    @Override
    public Mono<RagFlowContext> advise(RagFlowContext context) {
        context.promptModel()
                .put("current_date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return Mono.just(context);
    }
}
