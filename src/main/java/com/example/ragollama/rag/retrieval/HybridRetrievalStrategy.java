package com.example.ragollama.rag.retrieval;

import com.example.ragollama.monitoring.KnowledgeGapService;
import com.example.ragollama.rag.agent.ProcessedQueries;
import com.example.ragollama.rag.domain.retrieval.DocumentFtsRepository;
import com.example.ragollama.rag.domain.retrieval.FusionService;
import com.example.ragollama.rag.domain.retrieval.VectorSearchService;
import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.security.AccessControlService;
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
 * Эта версия выполняет векторный и полнотекстовый поиск параллельно,
 * применяет фильтры безопасности на основе ролей пользователя и
 * интеллектуально использует различные формы запроса (основной,
 * расширенные, оригинальный) для каждого типа поиска, повышая
 * производительность и релевантность.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalStrategy {

    private final VectorSearchService vectorSearchService;
    private final DocumentFtsRepository ftsRepository;
    private final FusionService fusionService;
    private final RetrievalProperties retrievalProperties;
    private final AsyncTaskExecutor applicationTaskExecutor;
    private final MetricService metricService;
    private final KnowledgeGapService knowledgeGapService;
    private final AccessControlService accessControlService;

    /**
     * Асинхронно извлекает и сливает документы, применяя фильтры безопасности.
     *
     * @param processedQueries    Объект с основным и расширенными запросами.
     * @param originalQuery       Оригинальный запрос пользователя для FTS.
     * @param topK                Количество документов для извлечения.
     * @param similarityThreshold Порог схожести.
     * @param businessFilter      Опциональный бизнес-фильтр (например, по типу документа).
     * @return {@link Mono} с финальным списком релевантных и разрешенных для пользователя документов.
     */
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

        Filter.Expression securityFilter = accessControlService.buildAccessFilter();
        Filter.Expression finalFilter = combineFilters(securityFilter, businessFilter);

        // Запускаем все поиски параллельно на выделенном пуле потоков
        Mono<List<Document>> primaryVectorSearch = executeVectorSearch(List.of(processedQueries.primaryQuery()), topK, similarityThreshold, finalFilter);
        Mono<List<Document>> expansionVectorSearch = executeVectorSearch(processedQueries.expansionQueries(), topK, similarityThreshold, finalFilter);
        Mono<List<Document>> ftsSearch = executeFtsSearch(originalQuery);

        return Mono.zip(primaryVectorSearch, expansionVectorSearch, ftsSearch)
                .map(tuple -> {
                    List<List<Document>> searchResults = List.of(tuple.getT1(), tuple.getT2(), tuple.getT3());
                    log.debug("Получено документов: точный={}, расширенный={}, fts={}", tuple.getT1().size(), tuple.getT2().size(), tuple.getT3().size());
                    return fusionService.reciprocalRankFusion(searchResults);
                });
    }

    private Mono<List<Document>> executeVectorSearch(List<String> queries, int topK, double similarityThreshold, @Nullable Filter.Expression filter) {
        if (queries == null || queries.isEmpty()) {
            return Mono.just(List.of());
        }
        return Mono.fromCallable(() ->
                        queries.parallelStream() // Безопасно, т.к. выполняется внутри Schedulers.fromExecutor
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

    private Filter.Expression combineFilters(Filter.Expression securityFilter, @Nullable Filter.Expression businessFilter) {
        if (businessFilter == null) {
            return securityFilter;
        }
        return new Filter.Expression(Filter.ExpressionType.AND, securityFilter, businessFilter);
    }
}
