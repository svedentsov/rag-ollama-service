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
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    private final AsyncTaskExecutor applicationTaskExecutor;

    /**
     * Асинхронно выполняет гибридный поиск.
     *
     * @param processedQueries    Объект с обработанными запросами.
     * @param originalQuery       Оригинальный запрос пользователя.
     * @param topK                Количество документов для извлечения.
     * @param similarityThreshold Порог схожести.
     * @param businessFilter      Опциональный бизнес-фильтр.
     * @return {@link Mono} со списком документов.
     */
    public Mono<List<Document>> retrieve(
            @Nullable ProcessedQueries processedQueries,
            String originalQuery,
            int topK,
            double similarityThreshold,
            @Nullable Filter.Expression businessFilter) {

        if (processedQueries == null || processedQueries.primaryQuery().isBlank()) {
            knowledgeGapService.recordGap(originalQuery);
            return Mono.just(List.of());
        }

        Mono<List<Document>> vectorSearchMono = vectorSearchService.search(processedQueries.expansionQueries(), topK, similarityThreshold, businessFilter, null)
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));

        Mono<List<Document>> ftsSearchMono = ftsSearchService.search(originalQuery);
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
                    List<Document> fusedDocs = fusionService.reciprocalRankFusion(List.of(vectorResults, ftsResults, graphResults));
                    if (fusedDocs.isEmpty()) {
                        knowledgeGapService.recordGap(originalQuery);
                    }
                    return fusedDocs;
                });
    }

    private boolean isGraphQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("связан") || lowerQuery.contains("какие тесты") || lowerQuery.contains("какие требования");
    }
}
