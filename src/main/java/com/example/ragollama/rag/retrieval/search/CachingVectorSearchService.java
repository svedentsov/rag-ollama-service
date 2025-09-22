package com.example.ragollama.rag.retrieval.search;

import com.example.ragollama.shared.metrics.MetricService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Декоратор, добавляющий кэширование к сервису векторного поиска.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachingVectorSearchService implements VectorSearchService {

    private final VectorSearchService delegate;
    private final MetricService metricService;

    /**
     * Выполняет поиск, используя кэш.
     * <p>
     * Аннотация {@code @Cacheable} инструктирует Spring перед выполнением этого
     * метода проверить наличие результата в кэше 'vector_search_results' по ключу,
     * сгенерированному {@code searchRequestKeyGenerator}. Если результат найден,
     * метод не будет выполнен, а будет возвращен кэшированный объект. В противном
     * случае, метод будет выполнен, а его результат — помещен в кэш.
     *
     * @param queries             Список текстов запросов.
     * @param topK                Количество извлекаемых документов.
     * @param similarityThreshold Порог схожести.
     * @param filter              Опциональный фильтр метаданных.
     * @return {@link List} найденных документов.
     */
    @Override
    @Cacheable(value = "vector_search_results", keyGenerator = "searchRequestKeyGenerator")
    public List<Document> search(List<String> queries, int topK, double similarityThreshold, @Nullable Filter.Expression filter, @Nullable Integer efSearch) {
        log.warn("ПРОМАХ КЭША: Выполнение реального векторного поиска для запроса: '{}' с efSearch={}", queries.get(0), efSearch);
        metricService.incrementCacheMiss();
        return delegate.search(queries, topK, similarityThreshold, filter, efSearch);
    }
}
