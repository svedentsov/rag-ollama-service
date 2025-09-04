package com.example.ragollama.rag.domain.reranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Сервис-оркестратор для переранжирования документов.
 * <p>Реализует паттерн "Компоновщик". Он не содержит собственной логики ранжирования,
 * а делегирует эту задачу списку внедренных бинов {@link RerankingStrategy}.
 * Spring автоматически собирает все такие бины и упорядочивает их согласно
 * аннотации {@code @Order}.
 * <p>Сервис активируется глобальным свойством {@code app.reranking.enabled=true}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.reranking.enabled", havingValue = "true")
public class RerankingService {

    private final List<RerankingStrategy> strategies;

    /**
     * Выполняет переранжирование списка документов.
     *
     * @param documents     Список документов, полученных после этапа слияния (fusion).
     * @param originalQuery Исходный запрос пользователя, который может использоваться стратегиями.
     * @return Финальный, переранжированный и отсортированный список документов.
     */
    public List<Document> rerank(List<Document> documents, String originalQuery) {
        if (documents == null || documents.isEmpty() || strategies.isEmpty()) {
            return documents;
        }

        // 1. Инициализация начальной оценки
        documents.forEach(doc -> {
            float originalSimilarity = doc.getMetadata().get("distance") != null
                    ? 1 - (float) doc.getMetadata().get("distance")
                    : 0f;
            doc.getMetadata().put("rerankedSimilarity", originalSimilarity);
            doc.getMetadata().put("originalSimilarity", originalSimilarity);
        });

        // 2. Последовательное применение всех активных стратегий
        log.info("Применение {} стратегий переранжирования.", strategies.size());
        List<Document> currentDocs = documents;
        for (RerankingStrategy strategy : strategies) {
            currentDocs = strategy.apply(currentDocs, originalQuery);
        }

        // 3. Финальная сортировка по итоговой оценке
        return currentDocs.stream()
                .sorted(Comparator.comparing(
                        doc -> (Float) doc.getMetadata().get("rerankedSimilarity"),
                        Comparator.reverseOrder()
                ))
                .toList();
    }
}
