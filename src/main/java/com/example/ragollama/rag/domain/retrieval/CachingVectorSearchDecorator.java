package com.example.ragollama.rag.domain.retrieval;

import com.example.ragollama.rag.retrieval.search.VectorSearchService;
import com.example.ragollama.shared.metrics.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Декоратор для {@link VectorSearchService}, добавляющий слой кэширования.
 *
 * <p>Этот класс оборачивает основную реализацию поиска и применяет кэширование
 * с помощью аннотации {@code @Cacheable}. Он делегирует фактический поиск
 * вложенному сервису только в случае промаха кэша.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachingVectorSearchDecorator implements VectorSearchService {

    private final VectorSearchService delegate; // Делегат (DefaultVectorSearchService)
    private final MetricService metricService;

    /**
     * {@inheritDoc}
     *
     * <p>Результаты этого метода кэшируются. Ключ для кэша генерируется
     * автоматически Spring'ом на основе всех аргументов.
     * При промахе кэша вызов делегируется основной реализации.
     */
    @Override
    @Cacheable(
            value = "vector_search_results",
            key = "{#queries, #topK, #similarityThreshold, T(com.example.ragollama.shared.util.FilterExpressionKeyHelper).generateKey(#filter)}"
    )
    public Mono<List<Document>> search(List<String> queries, int topK, double similarityThreshold, Filter.Expression filter) {
        log.info("Промах кэша. Делегирование векторного поиска для запросов: {}", queries);
        metricService.incrementCacheMiss();
        return delegate.search(queries, topK, similarityThreshold, filter);
    }
}
