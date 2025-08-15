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
 * Отделение этой логики позволяет лучше управлять кэшированием и тестированием
 * этапа извлечения данных (Retrieval).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private final VectorStore vectorStore;
    private final MetricService metricService;

    /**
     * Выполняет поиск по схожести в векторном хранилище.
     * <p>
     * Результаты этого метода кэшируются, так как это самая дорогая
     * и при этом детерминированная часть RAG-конвейера.
     * При промахе кэша инкрементируется соответствующая метрика.
     *
     * @param request Параметры поиска.
     * @return Список найденных документов.
     */
    @Cacheable(value = "vector_search_results", key = "#request.query + '_' + #request.topK + '_' + #request.similarityThreshold")
    public List<Document> search(SearchRequest request) {
        log.info("Кэш промахнулся. Выполняется поиск векторов для запроса: '{}'", request.getQuery());
        metricService.incrementCacheMiss(); // Теперь метрика вызывается в правильном месте
        return metricService.recordTimer("rag.retrieval",
                () -> vectorStore.similaritySearch(request)
        );
    }
}
