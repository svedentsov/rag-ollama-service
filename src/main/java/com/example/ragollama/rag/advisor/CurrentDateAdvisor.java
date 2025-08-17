package com.example.ragollama.rag.advisor;

import com.example.ragollama.rag.RagAdvisor;
import com.example.ragollama.rag.RagContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Простой советник, который добавляет текущую дату в модель промпта.
 * <p>
 * Реализует новый асинхронный интерфейс {@link RagAdvisor}, возвращая
 * результат, обернутый в {@link Mono}. Так как операция синхронна,
 * используется {@code Mono.just()} для немедленного возврата результата.
 */
@Component
@Order(10) // Задаем порядок выполнения
public class CurrentDateAdvisor implements RagAdvisor {

    /**
     * Добавляет текущую дату в модель промпта.
     *
     * @param context Текущий контекст запроса.
     * @return {@link Mono}, немедленно завершающийся с обновленным контекстом.
     */
    @Override
    public Mono<RagContext> advise(RagContext context) {
        context.getPromptModel()
                .put("current_date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return Mono.just(context);
    }
}
