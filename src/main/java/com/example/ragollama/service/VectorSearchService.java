package com.example.ragollama.service;

import com.example.ragollama.exception.RetrievalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис, инкапсулирующий логику взаимодействия с векторным хранилищем.
 * Является "антикоррупционным слоем", который изолирует остальную часть приложения
 * от деталей реализации и возможных сбоев {@link VectorStore}.
 * Результаты поиска кэшируются для повышения производительности.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final VectorStore vectorStore;
    private final MetricService metricService;

    /**
     * Выполняет поиск по схожести в векторном хранилище.
     * Результаты этого метода кэшируются. Ключ для кэша генерируется
     * с помощью кастомного бина {@code searchRequestKeyGenerator}.
     * В случае сбоя взаимодействия с хранилищем, выбрасывается доменное
     * исключение {@link RetrievalException}.
     *
     * @param request Параметры поиска.
     * @return Список найденных документов.
     * @throws RetrievalException если произошла ошибка при доступе к векторному хранилищу.
     */
    @Cacheable(value = "vector_search_results", keyGenerator = "searchRequestKeyGenerator")
    public List<Document> search(SearchRequest request) {
        try {
            log.info("Промах кэша. Выполняется поиск векторов для запроса: '{}'", request.getQuery());
            metricService.incrementCacheMiss();
            return metricService.recordTimer("rag.retrieval.vectors",
                    () -> vectorStore.similaritySearch(request));
        } catch (DataAccessException e) {
            log.error("Ошибка доступа к векторному хранилищу при выполнении запроса: '{}'", request.getQuery(), e);
            throw new RetrievalException("Не удалось выполнить поиск в векторном хранилище.", e);
        }
    }
}
