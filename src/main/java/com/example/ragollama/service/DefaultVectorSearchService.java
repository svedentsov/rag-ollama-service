package com.example.ragollama.service;

import com.example.ragollama.exception.RetrievalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Базовая реализация {@link VectorSearchService}, отвечающая исключительно
 * за прямое взаимодействие с {@link VectorStore}.
 * <p>
 * Этот класс свободен от аннотаций кэширования, что делает его легко
 * тестируемым в изоляции и в интеграционных тестах с Testcontainers.
 */
@Slf4j
@Service("defaultVectorSearchService") // Явно именуем бин для точной инъекции
@RequiredArgsConstructor
public class DefaultVectorSearchService implements VectorSearchService {

    private final VectorStore vectorStore;
    private final MetricService metricService;

    /**
     * {@inheritDoc}
     * <p>
     * Оборачивает вызов к {@link VectorStore} для сбора метрик и обработки
     * специфичных для хранилища исключений, преобразуя их в доменное
     * исключение {@link RetrievalException}.
     *
     * @throws RetrievalException если произошла ошибка при доступе к векторному хранилищу.
     */
    @Override
    public List<Document> search(SearchRequest request) {
        try {
            log.debug("Выполняется прямой поиск векторов для запроса: '{}'", request.getQuery());
            return metricService.recordTimer("rag.retrieval.vectors",
                    () -> vectorStore.similaritySearch(request));
        } catch (DataAccessException e) {
            log.error("Ошибка доступа к векторному хранилищу при выполнении запроса: '{}'", request.getQuery(), e);
            throw new RetrievalException("Не удалось выполнить поиск в векторном хранилище.", e);
        }
    }
}
