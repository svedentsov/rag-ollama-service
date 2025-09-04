package com.example.ragollama.rag.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link ContextArrangementStrategy}, применяющая стратегию "Сэндвич" (Sandwich/Reordering).
 * <p>Эта стратегия борется с проблемой "Lost in the Middle" у LLM, помещая
 * самые релевантные документы в начало и конец списка, где у модели
 * максимальное "внимание". Наименее релевантные документы оказываются в середине.
 * <p>Активируется при {@code app.rag.arrangement-strategy=sandwich}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.rag.arrangement-strategy", havingValue = "sandwich")
public class SandwichArrangementStrategy implements ContextArrangementStrategy {

    /**
     * Конструктор, логирующий активацию стратегии.
     */
    public SandwichArrangementStrategy() {
        log.info("Активирована стратегия компоновки контекста: SandwichArrangementStrategy");
    }

    /**
     * Переупорядочивает список документов для стратегии "Сэндвич".
     * <p>Пример: [d1, d2, d3, d4, d5, d6] -> [d1, d6, d2, d5, d3, d4]
     *
     * @param sortedDocs Исходный список, отсортированный по убыванию релевантности.
     * @return Новый список, переупорядоченный для оптимального восприятия LLM.
     */
    @Override
    public List<Document> arrange(List<Document> sortedDocs) {
        if (sortedDocs == null || sortedDocs.size() < 3) {
            return sortedDocs; // Стратегия не имеет смысла для 1-2 документов
        }

        List<Document> reordered = new ArrayList<>();
        int left = 0;
        int right = sortedDocs.size() - 1;

        while (left <= right) {
            if (left <= right) {
                reordered.add(sortedDocs.get(left++)); // Добавляем самый релевантный
            }
            if (left <= right) {
                reordered.add(sortedDocs.get(right--)); // Добавляем наименее релевантный (из текущего списка)
            }
        }
        log.debug("Документы переупорядочены для стратегии 'Сэндвич'. Исходный размер: {}, итоговый: {}.",
                sortedDocs.size(), reordered.size());
        return reordered;
    }
}
