package com.example.ragollama.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Сервис, инкапсулирующий логику взаимодействия с векторным хранилищем.
 * <p>
 * Реализует программное управление кэшированием для повышения
 * эффективности и сбора точных метрик по попаданиям и промахам.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private static final String CACHE_NAME = "vector_search_results";

    private final VectorStore vectorStore;
    private final MetricService metricService;
    private final CacheManager cacheManager;
    private final SearchRequestKeyGenerator keyGenerator;

    /**
     * Выполняет поиск по схожести в векторном хранилище с использованием кэша.
     *
     * @param request Параметры поиска.
     * @return Список найденных документов.
     */
    public List<Document> search(SearchRequest request) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE_NAME), "Кэш 'vector_search_results' не найден!");
        // Вызываем обновленный, типизированный метод генератора
        String key = keyGenerator.generate(request);

        // 1. Пытаемся получить значение из кэша
        return Optional.ofNullable(cache.get(key, List.class))
                .map(cachedDocuments -> {
                    // 2. ПОПАДАНИЕ В КЭШ (Cache Hit)
                    log.debug("Попадание в кэш для запроса: '{}' (ключ: {})", request.getQuery(), key);
                    metricService.incrementCacheHit();
                    // Тип здесь уже List<Document> из-за generic-типа в cache.get()
                    return (List<Document>) cachedDocuments;
                })
                .orElseGet(() -> {
                    // 3. ПРОМАХ КЭША (Cache Miss)
                    log.info("Промах кэша для запроса: '{}' (ключ: {}). Выполняется поиск векторов.", request.getQuery(), key);
                    metricService.incrementCacheMiss();

                    // Выполняем дорогостоящую операцию поиска
                    List<Document> documents = metricService.recordTimer("rag.retrieval",
                            () -> vectorStore.similaritySearch(request)
                    );

                    // 4. Помещаем результат в кэш
                    cache.put(key, documents);
                    return documents;
                });
    }
}
