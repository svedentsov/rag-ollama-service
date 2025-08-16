package com.example.ragollama.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис, отвечающий исключительно за этап извлечения (Retrieval) в RAG-конвейере.
 * Инкапсулирует логику поиска по схожести в векторном хранилище и, опционально,
 * последующего переранжирования найденных результатов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetrievalService {

    private final VectorSearchService vectorSearchService;
    private final Optional<RerankingService> rerankingService;
    private final AsyncTaskExecutor taskExecutor;

    /**
     * Выполняет асинхронный поиск и переранжирование документов.
     *
     * @param query               Текст запроса для поиска.
     * @param topK                Количество документов для извлечения.
     * @param similarityThreshold Порог схожести.
     * @return {@link CompletableFuture}, который завершится списком отсортированных документов.
     */
    public CompletableFuture<List<Document>> retrieveAndRerank(String query, int topK, double similarityThreshold) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Начало этапа Retrieval для запроса: '{}'", query);
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();

            List<Document> similarDocuments = vectorSearchService.search(searchRequest);
            log.debug("Найдено {} документов после векторного поиска.", similarDocuments.size());

            // Применяем переранжирование, если сервис RerankingService активен
            return rerankingService
                    .map(service -> {
                        log.debug("Выполняется переранжирование для {} документов.", similarDocuments.size());
                        List<Document> reranked = service.rerank(similarDocuments, query);
                        log.info("Этап Retrieval завершен. После переранжирования осталось {} документов.", reranked.size());
                        return reranked;
                    })
                    .orElseGet(() -> {
                        log.info("Этап Retrieval завершен. Переранжирование отключено.");
                        return similarDocuments;
                    });
        }, taskExecutor);
    }
}
