package com.example.ragollama.rag.retrieval;

import com.example.ragollama.monitoring.KnowledgeGapService;
import com.example.ragollama.rag.agent.ProcessedQueries;
import com.example.ragollama.rag.domain.reranking.RerankingService;
import com.example.ragollama.rag.domain.retrieval.DocumentFtsRepository;
import com.example.ragollama.rag.domain.retrieval.FusionService;
import com.example.ragollama.rag.domain.retrieval.VectorSearchService;
import com.example.ragollama.shared.metrics.MetricService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
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
 * Эта версия дополнена поддержкой опциональной фильтрации по метаданным,
 * что позволяет выполнять более точные и эффективные поисковые запросы,
 * ограничивая поиск определенным подмножеством документов.
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
     * Асинхронно извлекает и ранжирует документы с поддержкой фильтрации.
     *
     * @param processedQueries    Объект с основным и расширенными запросами.
     * @param originalQuery       Оригинальный запрос пользователя для FTS и реранжирования.
     * @param topK                Количество документов для извлечения.
     * @param similarityThreshold Порог схожести.
     * @param filter              Опциональное выражение для фильтрации по метаданным.
     * @return {@link Mono} с финальным списком релевантных документов.
     */
    public Mono<List<Document>> retrieve(
            ProcessedQueries processedQueries,
            String originalQuery,
            int topK,
            double similarityThreshold,
            @Nullable Filter.Expression filter) {

        if (processedQueries == null || processedQueries.primaryQuery().isBlank()) {
            knowledgeGapService.recordGap(originalQuery);
            return Mono.just(List.of());
        }

        Mono<List<Document>> vectorSearchMono = executeAdaptiveVectorSearch(processedQueries, topK, similarityThreshold, filter);
        Mono<List<Document>> ftsSearchMono = executeFtsSearch(originalQuery);

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

    private Mono<List<Document>> executeAdaptiveVectorSearch(ProcessedQueries queries, int topK, double threshold, @Nullable Filter.Expression filter) {
        log.info("Запуск адаптивной стратегии извлечения. Основной запрос: '{}'", queries.primaryQuery());
        Mono<List<Document>> primarySearchMono = executeVectorSearch(List.of(queries.primaryQuery()), topK, threshold, filter);

        return primarySearchMono.flatMap(primaryResults -> {
            int minDocsThreshold = retrievalProperties.hybrid().expansionMinDocsThreshold();
            if (primaryResults.size() >= minDocsThreshold || queries.expansionQueries().isEmpty()) {
                log.info("Найдено достаточно ({}) документов на первом шаге. Расширение поиска не требуется.", primaryResults.size());
                return Mono.just(primaryResults);
            }

            log.info("Найдено {}/{} документов. Запуск расширенного поиска по {} запросам.",
                    primaryResults.size(), minDocsThreshold, queries.expansionQueries().size());
            Mono<List<Document>> expansionSearchMono = executeVectorSearch(queries.expansionQueries(), topK, threshold, filter);

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

    private Mono<List<Document>> executeVectorSearch(List<String> queries, int topK, double similarityThreshold, @Nullable Filter.Expression filter) {
        return Mono.fromCallable(() ->
                        queries.parallelStream()
                                .flatMap(query -> {
                                    SearchRequest.Builder requestBuilder = SearchRequest.builder()
                                            .query(query)
                                            .topK(topK)
                                            .similarityThreshold(similarityThreshold);
                                    if (filter != null) {
                                        requestBuilder.filterExpression(filter);
                                    }
                                    return metricService.recordTimer("rag.retrieval.vectors.single", () ->
                                            vectorSearchService.search(requestBuilder.build())
                                    ).stream();
                                })
                                .distinct()
                                .toList()
                )
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));
    }

    private Mono<List<Document>> executeFtsSearch(String query) {
        int ftsTopK = retrievalProperties.hybrid().fts().topK();
        return Mono.fromCallable(() ->
                        metricService.recordTimer("rag.retrieval.fts", () ->
                                ftsRepository.searchByKeywords(query, ftsTopK)
                        )
                )
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));
    }
}
