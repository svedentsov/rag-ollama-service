package com.example.ragollama.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Сервис, отвечающий за этап извлечения (Retrieval) в RAG-конвейере.
 * Его основная задача — асинхронно выполнять поиск релевантных документов
 * в векторном хранилище на основе запроса пользователя. Результат возвращается
 * в виде {@link Mono} для нативной интеграции в реактивные цепочки обработки.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final VectorSearchService vectorSearchService;
    private final AsyncTaskExecutor taskExecutor;

    /**
     * Асинхронно выполняет поиск документов в векторном хранилище.
     * Так как взаимодействие с векторной БД через {@link VectorSearchService}
     * является блокирующей операцией (из-за JDBC), этот метод выполняет
     * ключевую архитектурную роль: он безопасно переносит выполнение
     * блокирующего кода в отдельный, управляемый пул потоков.
     * Это делается с помощью {@code Mono.fromCallable} и {@code subscribeOn},
     * что предотвращает блокировку чувствительных к блокировкам потоков Reactor (event-loop).
     *
     * @param query               Текст запроса пользователя для поиска.
     * @param topK                Максимальное количество наиболее релевантных документов для извлечения.
     * @param similarityThreshold Минимальный порог схожести (0.0-1.0), которому должны
     *                            соответствовать извлекаемые документы.
     * @return {@link Mono}, который по завершении асинхронной операции эммитит
     * список найденных документов {@link Document} или ошибку.
     */
    public Mono<List<Document>> retrieveDocuments(String query, int topK, double similarityThreshold) {
        log.info("Начало этапа Retrieval для запроса: '{}'", query);
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
        return Mono.fromCallable(() -> vectorSearchService.search(request))
                .subscribeOn(Schedulers.fromExecutor(taskExecutor));
    }
}
