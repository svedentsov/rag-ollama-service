package com.example.ragollama.rag.retrieval;

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
 * Эта стратегия сначала выполняет высокоточный поиск по основному
 * трансформированному запросу. Если найдено достаточно документов,
 * процесс завершается. В противном случае, запускается дополнительный
 * поиск по расширенному набору запросов для повышения полноты (recall).
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

    /**
     * Асинхронно извлекает и ранжирует документы.
     *
     * @param enhancedQueries Список запросов, сгенерированный конвейером. Предполагается,
     *                        что первый запрос в списке - самый точный (трансформированный).
     * @param originalQuery   Оригинальный запрос пользователя.
     * @return {@link Mono} с финальным списком релевантных документов.
     */
    public Mono<List<Document>> retrieve(List<String> enhancedQueries, String originalQuery) {
        if (enhancedQueries == null || enhancedQueries.isEmpty()) {
            return Mono.just(List.of());
        }

        String primaryQuery = enhancedQueries.getFirst();
        List<String> expansionQueries = enhancedQueries.size() > 1 ? enhancedQueries.subList(1, enhancedQueries.size()) : List.of();

        log.info("Запуск адаптивной стратегии извлечения. Основной запрос: '{}'", primaryQuery);

        Mono<List<Document>> primarySearchMono = executeVectorSearch(List.of(primaryQuery));

        return primarySearchMono.flatMap(primaryResults -> {
                    int minDocsThreshold = retrievalProperties.hybrid().expansionMinDocsThreshold();
                    if (primaryResults.size() >= minDocsThreshold || expansionQueries.isEmpty()) {
                        log.info("Найдено достаточно ({}) документов на первом шаге. Расширение поиска не требуется.", primaryResults.size());
                        return Mono.just(primaryResults);
                    }

                    log.info("Найдено {}/{} документов. Запуск расширенного поиска по {} запросам.",
                            primaryResults.size(), minDocsThreshold, expansionQueries.size());
                    Mono<List<Document>> expansionSearchMono = executeVectorSearch(expansionQueries);
                    return Mono.zip(Mono.just(primaryResults), expansionSearchMono)
                            .map(tuple -> {
                                List<Document> combined = new ArrayList<>(tuple.getT1());
                                combined.addAll(tuple.getT2());
                                // Дедупликация по ID документа
                                return combined.stream()
                                        .filter(d -> d.getId() != null)
                                        .collect(Collectors.toMap(Document::getId, d -> d, (d1, d2) -> d1))
                                        .values().stream().toList();
                            });
                }).flatMap(vectorResults -> {
                    Mono<List<Document>> ftsSearchMono = executeFtsSearch(originalQuery);
                    return Mono.zip(Mono.just(vectorResults), ftsSearchMono)
                            .map(tuple -> fusionService.reciprocalRankFusion(List.of(tuple.getT1(), tuple.getT2())));
                }).map(fusedDocs -> rerankingService.rerank(fusedDocs, originalQuery))
                .doOnSuccess(finalList -> {
                    log.info("Гибридный поиск завершен. Финальный список содержит {} документов.", finalList.size());
                    metricService.recordRetrievedDocumentsCount(finalList.size());
                });
    }

    /**
     * Параллельно выполняет векторный поиск для каждого запроса из списка.
     *
     * @param queries Список запросов для поиска.
     * @return {@link Mono} со списком уникальных найденных документов.
     */
    private Mono<List<Document>> executeVectorSearch(List<String> queries) {
        var config = retrievalProperties.hybrid().vectorSearch();
        return Mono.fromCallable(() ->
                        queries.parallelStream()
                                .flatMap(query -> {
                                    SearchRequest request = SearchRequest.builder()
                                            .query(query)
                                            .topK(config.topK())
                                            .similarityThreshold(config.similarityThreshold())
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
     *
     * @param query Оригинальный запрос пользователя.
     * @return {@link Mono} со списком найденных документов.
     */
    private Mono<List<Document>> executeFtsSearch(String query) {
        var config = retrievalProperties.hybrid().fts();
        return Mono.fromCallable(() ->
                        metricService.recordTimer("rag.retrieval.fts", () ->
                                ftsRepository.searchByKeywords(query, config.topK())
                        )
                )
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));
    }
}
