package com.example.ragollama.service;

import com.example.ragollama.config.properties.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.reranking.enabled", havingValue = "true")
public class RerankingService {

    private final double keywordMatchBoost;

    /**
     * Конструктор сервиса.
     *
     * @param appProperties "Усилитель" для документов, содержащих ключевые слова из запроса.
     *                      Значение задается в {@code application.yml}.
     */
    public RerankingService(AppProperties appProperties) {
        this.keywordMatchBoost = appProperties.reranking().keywordMatchBoost();
        log.info("RerankingService enabled with keyword match boost: {}", keywordMatchBoost);
    }

    /**
     * Выполняет переранжирование списка документов.
     * Реализует простую стратегию: повышает релевантность документов,
     * если их текст содержит ключевые слова из оригинального запроса.
     * Новая оценка схожести сохраняется в метаданных документа.
     *
     * @param documents     Список документов, полученных от векторного поиска.
     * @param originalQuery Исходный запрос пользователя.
     * @return Переранжированный список документов, отсортированный по новой оценке.
     */
    public List<Document> rerank(List<Document> documents, String originalQuery) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<String> queryKeywords = Arrays.asList(originalQuery.toLowerCase().split("\\s+"));
        return documents.stream()
                .peek(doc -> {
                    float originalSimilarity = doc.getMetadata().get("distance") != null ? 1 - (float) doc.getMetadata().get("distance") : 0f;
                    long matchCount = queryKeywords.stream()
                            .filter(kw -> doc.getText().toLowerCase().contains(kw))
                            .count();
                    float boost = (float) (matchCount * keywordMatchBoost);
                    float newSimilarity = originalSimilarity + boost;
                    doc.getMetadata().put("rerankedSimilarity", Math.min(1.0f, newSimilarity));
                    doc.getMetadata().put("originalSimilarity", originalSimilarity);
                })
                .sorted((d1, d2) -> {
                    Float sim1 = (Float) d1.getMetadata().get("rerankedSimilarity");
                    Float sim2 = (Float) d2.getMetadata().get("rerankedSimilarity");
                    return sim2.compareTo(sim1);
                })
                .toList();
    }
}
