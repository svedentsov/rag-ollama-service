package com.example.ragollama.rag.advisor;

import com.example.ragollama.rag.RagAdvisor;
import com.example.ragollama.rag.RagContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Простой советник, который добавляет текущую дату в модель промпта.
 * Это демонстрирует, как легко можно обогащать контекст RAG-запроса
 * дополнительной информацией без изменения основного конвейера.
 * Аннотация {@code @Order} задает приоритет выполнения (меньшее значение - выше приоритет).
 */
@Component
@Order(10) // Задаем порядок выполнения
public class CurrentDateAdvisor implements RagAdvisor {
    @Override
    public RagContext advise(RagContext context) {
        context.getPromptModel()
                .put("current_date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return context;
    }
}
