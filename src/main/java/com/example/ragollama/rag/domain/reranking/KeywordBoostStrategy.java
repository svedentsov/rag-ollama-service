package com.example.ragollama.rag.domain.reranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Стратегия переранжирования, которая повышает оценку документов, содержащих
 * точные ключевые слова из оригинального запроса.
 * <p>
 * Эта стратегия полезна для "гибридизации" семантического поиска, добавляя
 * к нему элемент лексического (keyword) соответствия.
 * Активируется свойством {@code app.reranking.strategies.keyword-boost.enabled=true}.
 */
@Slf4j
@Component
@Order(10)
@ConditionalOnProperty(name = "app.reranking.strategies.keyword-boost.enabled", havingValue = "true")
@RequiredArgsConstructor
public class KeywordBoostStrategy implements RerankingStrategy {

    private final RerankingProperties properties;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Document> apply(List<Document> documents, String originalQuery) {
        final double boostFactor = properties.strategies().keywordBoost().boostFactor();
        if (boostFactor <= 0) {
            return documents;
        }

        Set<String> queryKeywords = Arrays.stream(originalQuery.toLowerCase().split("\\s+"))
                .filter(kw -> kw.length() > 2) // Игнорируем короткие слова (предлоги, союзы)
                .collect(Collectors.toSet());

        if (queryKeywords.isEmpty()) {
            return documents;
        }

        log.debug("Применение KeywordBoostStrategy с фактором {} и ключевыми словами: {}", boostFactor, queryKeywords);

        documents.forEach(doc -> {
            String docTextLower = doc.getText().toLowerCase();
            long matchCount = queryKeywords.stream()
                    .filter(docTextLower::contains)
                    .count();

            float boost = (float) (matchCount * boostFactor);
            if (boost > 0) {
                float currentSimilarity = (float) doc.getMetadata().get("rerankedSimilarity");
                float newSimilarity = currentSimilarity + boost;
                doc.getMetadata().put("rerankedSimilarity", newSimilarity);
                log.trace("Документ ID {} получил буст: {} (было: {}, стало: {})", doc.getId(), boost, currentSimilarity, newSimilarity);
            }
        });

        return documents;
    }
}
