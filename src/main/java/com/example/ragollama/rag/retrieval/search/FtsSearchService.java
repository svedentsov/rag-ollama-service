package com.example.ragollama.rag.retrieval.search;

import com.example.ragollama.rag.domain.retrieval.DocumentFtsRepository;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import com.example.ragollama.shared.aop.ResilientDatabaseOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Сервис, инкапсулирующий логику полнотекстового поиска (FTS).
 * *
 * <p>Этот сервис является отказоустойчивым и кэшируемым. Он защищен
 * от временных сбоев базы данных с помощью {@link ResilientDatabaseOperation}
 * и кэширует результаты для частых запросов с помощью {@code @Cacheable}.
 * *
 * <p>Логика поиска делегируется {@link DocumentFtsRepository}, а асинхронность
 * достигается за счет выполнения блокирующего вызова в выделенном пуле потоков.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FtsSearchService {

    private final DocumentFtsRepository ftsRepository;
    private final RetrievalProperties retrievalProperties;

    /**
     * Асинхронно выполняет полнотекстовый поиск.
     *
     * @param query Текст запроса.
     * @return {@link Mono} со списком найденных документов.
     */
    public Mono<List<Document>> search(String query) {
        return Mono.fromCallable(() -> searchSync(query))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Синхронный, кэшируемый и отказоустойчивый метод для выполнения FTS.
     * *
     * <p>Этот метод является "ядром" сервиса. Аннотации {@code @Cacheable} и
     * {@code @ResilientDatabaseOperation} декларативно добавляют необходимую
     * функциональность без усложнения бизнес-логики.
     *
     * @param query Текст запроса.
     * @return Список найденных документов.
     */
    @Cacheable("fts_search_results")
    @ResilientDatabaseOperation
    public List<Document> searchSync(String query) {
        log.info("Промах кэша FTS. Выполнение полнотекстового поиска для: '{}'", query);
        int ftsTopK = retrievalProperties.hybrid().fts().topK();
        return ftsRepository.searchByKeywords(query, ftsTopK);
    }
}
