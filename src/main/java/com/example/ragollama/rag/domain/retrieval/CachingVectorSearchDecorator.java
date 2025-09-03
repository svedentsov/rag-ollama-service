package com.example.ragollama.rag.domain.retrieval;

import com.example.ragollama.shared.metrics.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Декоратор для {@link VectorSearchService}, добавляющий слой кэширования.
 *
 * <p>Этот класс оборачивает основную реализацию поиска и применяет кэширование
 * с помощью аннотации {@code @Cacheable}. Он делегирует фактический поиск
 * вложенному сервису только в случае промаха кэша.
 */
@Slf4j
@Service("cachingVectorSearchDecorator") // Явно именуем бин
@RequiredArgsConstructor
public class CachingVectorSearchDecorator implements VectorSearchService {

    private final VectorSearchService delegate; // Делегат (DefaultVectorSearchService)
    private final MetricService metricService;

    /**
     * {@inheritDoc}
     *
     * <p>Результаты этого метода кэшируются. Ключ для кэша генерируется
     * с помощью кастомного бина {@code searchRequestKeyGenerator}.
     * При промахе кэша вызов делегируется основной реализации.
     */
    @Override
    @Cacheable(value = "vector_search_results", keyGenerator = "searchRequestKeyGenerator")
    public List<Document> search(SearchRequest request) {
        log.info("Промах кэша. Делегирование поиска для запроса: '{}'", request.getQuery());
        metricService.incrementCacheMiss();
        return delegate.search(request);
    }
}
