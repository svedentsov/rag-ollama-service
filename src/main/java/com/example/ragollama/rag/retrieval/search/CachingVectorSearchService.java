package com.example.ragollama.rag.retrieval.search;

import com.example.ragollama.shared.metrics.MetricService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Декоратор, добавляющий кэширование к сервису векторного поиска, адаптированный для реактивного стека.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachingVectorSearchService implements VectorSearchService {

    private final VectorSearchService delegate;
    private final MetricService metricService;

    @Override
    @Cacheable(value = "vector_search_results", keyGenerator = "searchRequestKeyGenerator")
    public Mono<List<Document>> search(List<String> queries, int topK, double similarityThreshold, @Nullable Filter.Expression filter, @Nullable Integer efSearch) {
        log.warn("ПРОМАХ КЭША: Выполнение реального векторного поиска для запроса: '{}' с efSearch={}", queries.get(0), efSearch);
        metricService.incrementCacheMiss();
        // Делегируем выполнение и позволяем @Cacheable обернуть результат в Mono
        return delegate.search(queries, topK, similarityThreshold, filter, efSearch);
    }
}
