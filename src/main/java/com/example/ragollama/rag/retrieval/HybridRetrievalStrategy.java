package com.example.ragollama.rag.retrieval;

import com.example.ragollama.monitoring.KnowledgeGapService;
import com.example.ragollama.rag.agent.ProcessedQueries;
import com.example.ragollama.rag.retrieval.fusion.FusionService;
import com.example.ragollama.rag.retrieval.search.FtsSearchService;
import com.example.ragollama.rag.retrieval.search.GraphSearchService;
import com.example.ragollama.rag.retrieval.search.VectorSearchService;
import com.example.ragollama.shared.metrics.MetricService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Реализует адаптивную гибридную стратегию извлечения документов, включая графовый поиск.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalStrategy {

    private final VectorSearchService vectorSearchService;
    private final FtsSearchService ftsSearchService;
    private final FusionService fusionService;
    private final MetricService metricService;
    private final KnowledgeGapService knowledgeGapService;
    private final GraphSearchService graphSearchService;

    public Mono<List<Document>> retrieve(
            ProcessedQueries processedQueries,
            String originalQuery,
            int topK,
            double similarityThreshold,
            @Nullable Filter.Expression businessFilter) {

        if (processedQueries == null || processedQueries.primaryQuery().isBlank()) {
            knowledgeGapService.recordGap(originalQuery);
            return Mono.just(List.of());
        }
        // Запускаем все поиски параллельно
        Mono<List<Document>> vectorSearchMono = vectorSearchService.search(processedQueries.expansionQueries(), topK, similarityThreshold, businessFilter);
        Mono<List<Document>> ftsSearchMono = ftsSearchService.search(originalQuery);
        // Добавляем вызов графового поиска, если запрос релевантен
        Mono<List<Document>> graphSearchMono = isGraphQuery(originalQuery)
                ? graphSearchService.search(originalQuery)
                : Mono.just(List.of());
        return Mono.zip(vectorSearchMono, ftsSearchMono, graphSearchMono)
                .map(tuple -> {
                    List<Document> vectorResults = tuple.getT1();
                    List<Document> ftsResults = tuple.getT2();
                    List<Document> graphResults = tuple.getT3();
                    metricService.recordRetrievedDocumentsCount(vectorResults.size() + ftsResults.size() + graphResults.size());
                    if (!graphResults.isEmpty()) {
                        log.info("Получено {} результатов из Графа Знаний для запроса: '{}'", graphResults.size(), originalQuery);
                    }
                    // Объединяем результаты всех трех источников
                    return fusionService.reciprocalRankFusion(List.of(vectorResults, ftsResults, graphResults));
                });
    }

    // Простая эвристика для определения, является ли запрос графовым
    private boolean isGraphQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("связан") || lowerQuery.contains("какие тесты") || lowerQuery.contains("какие требования");
    }
}
