package com.example.ragollama.rag.retrieval.search;

import com.example.ragollama.shared.exception.RetrievalException;
import com.example.ragollama.shared.metrics.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Базовая реализация {@link VectorSearchService}, отвечающая исключительно
 * за прямое взаимодействие с {@link VectorStore}.
 * <p>
 * Эта версия возвращает синхронный результат, что необходимо для корректной
 * работы декларативного кэширования Spring.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultVectorSearchService implements VectorSearchService {

    private final VectorStore vectorStore;
    private final MetricService metricService;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Document> search(List<String> queries, int topK, double similarityThreshold, Filter.Expression filter) {
        // Выполняем поиск для каждого запроса и объединяем результаты
        return queries.stream()
                .parallel() // Распараллеливаем запросы для повышения производительности
                .flatMap(query -> performSingleSearch(query, topK, similarityThreshold, filter).stream())
                .distinct() // Удаляем дубликаты документов
                .toList();
    }

    /**
     * Выполняет один поисковый запрос к векторному хранилищу.
     *
     * @param query     Текст запроса.
     * @param topK      Количество извлекаемых документов.
     * @param threshold Порог схожести.
     * @param filter    Фильтр метаданных.
     * @return Список найденных документов.
     * @throws RetrievalException в случае ошибки доступа к данным.
     */
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
            log.error("Ошибка доступа к векторному хранилищу при выполнении запроса: '{}'", query, e);
            throw new RetrievalException("Не удалось выполнить поиск в векторном хранилище.", e);
        }
    }
}
