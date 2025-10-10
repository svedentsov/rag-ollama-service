package com.example.ragollama.rag.retrieval.search;

import com.example.ragollama.shared.exception.RetrievalException;
import com.example.ragollama.shared.metrics.MetricService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.dao.DataAccessException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Базовая реализация {@link VectorSearchService}, адаптированная для R2DBC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultVectorSearchService implements VectorSearchService {

    private final VectorStore vectorStore;
    private final MetricService metricService;
    private final DatabaseClient databaseClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<List<Document>> search(List<String> queries, int topK, double similarityThreshold, @Nullable Filter.Expression filter, @Nullable Integer efSearch) {
        Mono<Void> setupMono = Mono.empty();

        if (efSearch != null) {
            log.debug("Установка локального параметра hnsw.ef_search = {} для текущего запроса.", efSearch);
            setupMono = databaseClient.sql("SET LOCAL hnsw.ef_search = :efSearch")
                    .bind("efSearch", efSearch)
                    .then();
        }

        return setupMono.then(Mono.fromCallable(() ->
                queries.stream()
                        .parallel()
                        .flatMap(query -> performSingleSearch(query, topK, similarityThreshold, filter).stream())
                        .distinct()
                        .toList()
        ));
    }

    private List<Document> performSingleSearch(String query, int topK, double threshold, Filter.Expression filter) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query("query: " + query)
                    .topK(topK)
                    .similarityThreshold(threshold)
                    .filterExpression(filter)
                    .build();
            return metricService.recordTimer("rag.retrieval.vectors.single",
                    () -> vectorStore.similaritySearch(request));
        } catch (DataAccessException e) {
            log.error("Ошибка доступа к векторному хранилищу: '{}'", query, e);
            throw new RetrievalException("Не удалось выполнить поиск в векторном хранилище.", e);
        }
    }
}
