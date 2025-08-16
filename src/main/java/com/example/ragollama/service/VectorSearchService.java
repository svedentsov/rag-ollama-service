package com.example.ragollama.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис, инкапсулирующий логику взаимодействия с векторным хранилищем.
 * Результаты поиска кэшируются с помощью декларативной аннотации {@code @Cacheable},
 * что отделяет логику поиска от логики кэширования.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private final VectorStore vectorStore;
    private final MetricService metricService;

    /**
     * Выполняет поиск по схожести в векторном хранилище.
     * Результаты этого метода кэшируются. При первом вызове с уникальным
     * {@link SearchRequest} будет выполнен реальный запрос к VectorStore,
     * а результат сохранен в кэше 'vector_search_results'.
     * Для генерации ключа используется кастомный бин {@code vectorSearchCacheKeyGenerator}.
     *
     * @param request Параметры поиска.
     * @return Список найденных документов.
     */
    @Cacheable(value = "vector_search_results", keyGenerator = "vectorSearchCacheKeyGenerator")
    public List<Document> search(SearchRequest request) {
        log.info("Промах кэша. Выполняется поиск векторов для запроса: '{}'", request.getQuery());
        metricService.incrementCacheMiss();
        return metricService.recordTimer("rag.retrieval",
                () -> vectorStore.similaritySearch(request));
    }
}
