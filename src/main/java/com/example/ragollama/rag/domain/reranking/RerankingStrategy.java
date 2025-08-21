package com.example.ragollama.rag.domain.reranking;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Функциональный интерфейс, определяющий контракт для одной стратегии переранжирования.
 * <p>
 * Каждая реализация инкапсулирует отдельный фактор, влияющий на финальный
 * порядок документов (например, совпадение по ключевым словам, новизна, авторитетность источника).
 */
@FunctionalInterface
public interface RerankingStrategy {

    /**
     * Применяет свою логику к списку документов, модифицируя их метаданные
     * для влияния на финальную сортировку.
     * <p>
     * Стратегия должна прочитать существующую оценку (например, 'rerankedSimilarity'),
     * вычислить свой собственный "буст" (повышение) и добавить его к оценке.
     *
     * @param documents     Список документов для обработки.
     * @param originalQuery Оригинальный запрос пользователя.
     * @return Тот же список документов, но с обновленными метаданными.
     */
    List<Document> apply(List<Document> documents, String originalQuery);
}
