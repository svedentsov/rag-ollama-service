package com.example.ragollama.rag.domain.reranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(15)
@ConditionalOnProperty(name = "app.reranking.strategies.diversity.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DiversityRerankingStrategy implements RerankingStrategy {

    private final RerankingProperties properties;

    @Override
    public List<Document> apply(List<Document> documents, String originalQuery) {
        if (documents == null || documents.size() < 2) {
            return documents;
        }

        double lambda = properties.strategies().diversity().lambda();
        log.debug("Применение DiversityRerankingStrategy (MMR) с lambda={}", lambda);

        List<Document> remainingDocs = new ArrayList<>(documents);
        List<Document> rerankedDocs = new ArrayList<>();

        if (!remainingDocs.isEmpty()) {
            // Первый документ всегда самый релевантный
            rerankedDocs.add(remainingDocs.remove(0));
        }

        while (!remainingDocs.isEmpty()) {
            Document nextDoc = null;
            double maxMmrScore = Double.NEGATIVE_INFINITY;

            for (Document candidateDoc : remainingDocs) {
                float relevance = getRelevanceScore(candidateDoc);
                double maxSimilarityToSelected = rerankedDocs.stream()
                        .mapToDouble(selectedDoc -> cosineSimilarity(selectedDoc, candidateDoc))
                        .max()
                        .orElse(0.0);

                double mmrScore = lambda * relevance - (1 - lambda) * maxSimilarityToSelected;

                if (mmrScore > maxMmrScore) {
                    maxMmrScore = mmrScore;
                    nextDoc = candidateDoc;
                }
            }

            if (nextDoc != null) {
                rerankedDocs.add(nextDoc);
                remainingDocs.remove(nextDoc);
            } else {
                break; // Больше нечего добавлять
            }
        }
        return rerankedDocs;
    }

    private float getRelevanceScore(Document doc) {
        Object score = doc.getMetadata().get("rerankedSimilarity");
        if (score instanceof Float) {
            return (Float) score;
        }
        return 0.0f;
    }

    /**
     * ИСПРАВЛЕНИЕ: Извлекаем эмбеддинги из метаданных документа.
     */
    @SuppressWarnings("unchecked")
    private double cosineSimilarity(Document doc1, Document doc2) {
        // ИСПРАВЛЕНИЕ: Получаем эмбеддинг из метаданных
        List<Double> v1 = (List<Double>) doc1.getMetadata().get("embedding");
        List<Double> v2 = (List<Double>) doc2.getMetadata().get("embedding");

        if (v1 == null || v2 == null) {
            log.trace("Не удалось вычислить схожесть: один из эмбеддингов отсутствует.");
            return 0.0;
        }

        Assert.isTrue(v1.size() == v2.size(), "Векторы должны иметь одинаковую размерность");

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
