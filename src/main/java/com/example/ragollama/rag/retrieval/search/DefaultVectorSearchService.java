package com.example.ragollama.rag.retrieval.search;

import com.example.ragollama.shared.exception.RetrievalException;
import com.example.ragollama.shared.metrics.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Базовая реализация {@link VectorSearchService}, отвечающая исключительно
 * за прямое взаимодействие с {@link VectorStore}.
 * <p>
 * Этот класс свободен от аннотаций кэширования, что делает его легко
 * тестируемым в изоляции и в интеграционных тестах с Testcontainers.
 * Он выполняет блокирующие вызовы к `VectorStore` в выделенном пуле потоков,
 * обеспечивая асинхронность на уровне сервиса.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultVectorSearchService implements VectorSearchService {

    private final VectorStore vectorStore;
    private final MetricService metricService;
    private final AsyncTaskExecutor applicationTaskExecutor;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<List<Document>> search(List<String> queries, int topK, double similarityThreshold, Filter.Expression filter) {
        return Flux.fromIterable(queries)
                .parallel()
                .runOn(Schedulers.fromExecutor(applicationTaskExecutor))
                .flatMap(query -> Mono.fromCallable(() -> performSingleSearch(query, topK, similarityThreshold, filter)))
                .sequential()
                .flatMap(Flux::fromIterable)
                .distinct(Document::getId)
                .collectList()
                .doOnSuccess(docs -> log.debug("Параллельный векторный поиск завершен. Найдено {} уникальных документов.", docs.size()))
                .doOnError(e -> log.error("Ошибка во время параллельного векторного поиска.", e));
    }

    /**
     * Выполняет один поиск и оборачивает его в метрики и обработку ошибок.
     *
     * @param query     Текст запроса.
     * @param topK      Количество документов.
     * @param threshold Порог схожести.
     * @param filter    Фильтр.
     * @return Список найденных документов.
     * @throws RetrievalException если произошла ошибка доступа к данным.
     */
    private List<Document> performSingleSearch(String query, int topK, double threshold, Filter.Expression filter) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
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
