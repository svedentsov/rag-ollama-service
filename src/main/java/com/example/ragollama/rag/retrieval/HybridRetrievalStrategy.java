package com.example.ragollama.rag.retrieval;

import com.example.ragollama.monitoring.KnowledgeGapService;
import com.example.ragollama.rag.agent.ProcessedQueries;
import com.example.ragollama.rag.domain.reranking.RerankingService;
import com.example.ragollama.rag.domain.retrieval.DocumentFtsRepository;
import com.example.ragollama.rag.domain.retrieval.FusionService;
import com.example.ragollama.rag.domain.retrieval.VectorSearchService;
import com.example.ragollama.shared.metrics.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Реализует адаптивную гибридную стратегию извлечения документов.
 * <p>
 * Эта версия включает явный параллельный вызов полнотекстового поиска (FTS)
 * и интегрирует шаг переранжирования (Reranking) после слияния результатов
 * для повышения итоговой релевантности.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalStrategy {

    private final VectorSearchService vectorSearchService;
    private final DocumentFtsRepository ftsRepository;
    private final FusionService fusionService;
    private final RerankingService rerankingService;
    private final RetrievalProperties retrievalProperties;
    private final AsyncTaskExecutor applicationTaskExecutor;
    private final MetricService metricService;
    private final KnowledgeGapService knowledgeGapService;

    /**
     * Асинхронно извлекает и ранжирует документы.
     *
     * @param processedQueries    Объект с основным и расширенными запросами.
     * @param originalQuery       Оригинальный запрос пользователя.
     * @param topK                Количество документов для извлечения.
     * @param similarityThreshold Порог схожести для векторного поиска.
     * @return {@link Mono} с финальным списком релевантных документов.
     */
    public Mono<List<Document>> retrieve(ProcessedQueries processedQueries, String originalQuery, int topK, double similarityThreshold) {
        if (processedQueries == null || processedQueries.primaryQuery().isBlank()) {
            knowledgeGapService.recordGap(originalQuery);
            return Mono.just(List.of());
        }

        // --- Этап 1: Адаптивный векторный поиск ---
        Mono<List<Document>> vectorSearchMono = executeAdaptiveVectorSearch(processedQueries, topK, similarityThreshold);

        // --- Этап 2: Параллельный FTS-поиск ---
        Mono<List<Document>> ftsSearchMono = executeFtsSearch(originalQuery, topK);

        // --- Этап 3: Слияние и переранжирование ---
        return Mono.zip(vectorSearchMono, ftsSearchMono)
                .map(tuple -> {
                    log.debug("Получено {} док-ов от векторного поиска и {} от FTS.", tuple.getT1().size(), tuple.getT2().size());
                    return fusionService.reciprocalRankFusion(List.of(tuple.getT1(), tuple.getT2()));
                })
                .map(fusedDocs -> rerankingService.rerank(fusedDocs, originalQuery))
                .doOnSuccess(finalList -> {
                    log.info("Гибридный поиск завершен. Финальный список содержит {} документов.", finalList.size());
                    metricService.recordRetrievedDocumentsCount(finalList.size());
                    if (finalList.isEmpty()) {
                        knowledgeGapService.recordGap(originalQuery);
                    }
                });
    }

    /**
     * Выполняет адаптивный векторный поиск: сначала по основному запросу,
     * затем, при необходимости, по расширенным.
     */
    private Mono<List<Document>> executeAdaptiveVectorSearch(ProcessedQueries queries, int topK, double threshold) {
        log.info("Запуск адаптивной стратегии извлечения. Основной запрос: '{}'", queries.primaryQuery());
        Mono<List<Document>> primarySearchMono = executeVectorSearch(List.of(queries.primaryQuery()), topK, threshold);

        return primarySearchMono.flatMap(primaryResults -> {
            int minDocsThreshold = retrievalProperties.hybrid().expansionMinDocsThreshold();
            if (primaryResults.size() >= minDocsThreshold || queries.expansionQueries().isEmpty()) {
                log.info("Найдено достаточно ({}) документов на первом шаге. Расширение поиска не требуется.", primaryResults.size());
                return Mono.just(primaryResults);
            }

            log.info("Найдено {}/{} документов. Запуск расширенного поиска по {} запросам.",
                    primaryResults.size(), minDocsThreshold, queries.expansionQueries().size());
            Mono<List<Document>> expansionSearchMono = executeVectorSearch(queries.expansionQueries(), topK, threshold);

            return Mono.zip(Mono.just(primaryResults), expansionSearchMono)
                    .map(tuple -> {
                        List<Document> combined = new ArrayList<>(tuple.getT1());
                        combined.addAll(tuple.getT2());
                        return combined.stream()
                                .filter(d -> d.getId() != null)
                                .collect(Collectors.toMap(Document::getId, d -> d, (d1, d2) -> d1))
                                .values().stream().toList();
                    });
        });
    }

    /**
     * Параллельно выполняет векторный поиск для каждого запроса из списка.
     */
    private Mono<List<Document>> executeVectorSearch(List<String> queries, int topK, double similarityThreshold) {
        return Mono.fromCallable(() ->
                        queries.parallelStream()
                                .flatMap(query -> {
                                    SearchRequest request = SearchRequest.builder()
                                            .query(query)
                                            .topK(topK)
                                            .similarityThreshold(similarityThreshold)
                                            .build();
                                    return metricService.recordTimer("rag.retrieval.vectors.single", () ->
                                            vectorSearchService.search(request)
                                    ).stream();
                                })
                                .distinct()
                                .toList()
                )
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));
    }

    /**
     * Выполняет полнотекстовый поиск.
     */
    private Mono<List<Document>> executeFtsSearch(String query, int topK) {
        int ftsTopK = retrievalProperties.hybrid().fts().topK();
        return Mono.fromCallable(() ->
                        metricService.recordTimer("rag.retrieval.fts", () ->
                                ftsRepository.searchByKeywords(query, ftsTopK)
                        )
                )
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));
    }
}
