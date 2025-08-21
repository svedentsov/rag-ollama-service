package com.example.ragollama.rag.domain.reranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Стратегия переранжирования, которая повышает оценку документов, содержащих ключевые слова из оригинального запроса.
 * Активируется свойством {@code app.reranking.strategies.keyword-boost.enabled=true}.
 */
@Slf4j
@Component
@Order(10) // Будет применена первой
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
        if (boostFactor == 0) {
            return documents;
        }

        List<String> queryKeywords = Arrays.asList(originalQuery.toLowerCase().split("\\s+"));
        log.debug("Применение KeywordBoostStrategy с фактором {}", boostFactor);

        documents.forEach(doc -> {
            long matchCount = queryKeywords.stream()
                    .filter(kw -> doc.getText().toLowerCase().contains(kw))
                    .count();
            float boost = (float) (matchCount * boostFactor);
            if (boost > 0) {
                float currentSimilarity = (float) doc.getMetadata().get("rerankedSimilarity");
                doc.getMetadata().put("rerankedSimilarity", currentSimilarity + boost);
            }
        });

        return documents;
    }
}
