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

import java.util.List;

/**
 * Реализует адаптивную гибридную стратегию извлечения документов.
 * <p>
 * Эта версия использует по-настоящему параллельный подход. Она одновременно
 * выполняет точный поиск по основному запросу и расширяющие поиски по
 * альтернативным запросам, а затем интеллектуально объединяет все результаты.
 * Также добавлена поддержка опциональной фильтрации и логирование
 * "пробелов в знаниях".
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
     * Асинхронно извлекает и ранжирует документы, используя параллельный гибридный поиск.
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

        // 1. Параллельно запускаем все поиски: точный векторный, расширенный векторный и FTS
        Mono<List<Document>> primaryVectorSearch = executeVectorSearch(List.of(processedQueries.primaryQuery()), topK, similarityThreshold, filter);
        Mono<List<Document>> expansionVectorSearch = executeVectorSearch(processedQueries.expansionQueries(), topK, similarityThreshold, filter);
        Mono<List<Document>> ftsSearch = executeFtsSearch(originalQuery);

        // 2. Объединяем результаты всех трех потоков
        return Mono.zip(primaryVectorSearch, expansionVectorSearch, ftsSearch)
                .map(tuple -> {
                    List<List<Document>> searchResults = List.of(tuple.getT1(), tuple.getT2(), tuple.getT3());
                    log.debug("Получено документов: точный={}, расширенный={}, fts={}", tuple.getT1().size(), tuple.getT2().size(), tuple.getT3().size());
                    return fusionService.reciprocalRankFusion(searchResults);
                })
                .map(fusedDocs -> rerankingService.rerank(fusedDocs, originalQuery))
                .doOnSuccess(finalList -> {
                    log.info("Гибридный поиск завершен. Финальный список содержит {} документов.", finalList.size());
                    metricService.recordRetrievedDocumentsCount(finalList.size());
                    // 3. Если после всех усилий ничего не найдено, логируем "пробел в знаниях"
                    if (finalList.isEmpty()) {
                        knowledgeGapService.recordGap(originalQuery);
                    }
                });
    }

    /**
     * Выполняет параллельный векторный поиск для списка запросов.
     *
     * @param queries             Список текстовых запросов.
     * @param topK                Количество документов.
     * @param similarityThreshold Порог схожести.
     * @param filter              Опциональный фильтр.
     * @return {@link Mono} со списком уникальных найденных документов.
     */
    private Mono<List<Document>> executeVectorSearch(List<String> queries, int topK, double similarityThreshold, @Nullable Filter.Expression filter) {
        if (queries == null || queries.isEmpty()) {
            return Mono.just(List.of());
        }
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
                                .distinct() // Убираем дубликаты документов, найденные по разным запросам
                                .toList()
                )
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));
    }

    /**
     * Выполняет полнотекстовый поиск.
     */
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
