package com.example.ragollama.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Сервис, отвечающий за этап извлечения (Retrieval) в RAG-конвейере.
 * Реализует продвинутую стратегию гибридного поиска, параллельно выполняя
 * семантический (векторный) и лексический (полнотекстовый) поиск.
 * Результаты объединяются с помощью {@link FusionService} для получения
 * наиболее релевантного и полного набора документов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetrievalService {

    private final VectorSearchService vectorSearchService;
    private final FusionService fusionService;
    private final AsyncTaskExecutor taskExecutor;

    /**
     * Выполняет асинхронный гибридный поиск документов.
     *
     * @param query               Текст запроса для поиска.
     * @param topK                Количество документов для извлечения из каждого источника.
     * @param similarityThreshold Порог схожести для векторного поиска.
     * @return {@link CompletableFuture}, который завершится объединенным и отсортированным списком документов.
     */
    public CompletableFuture<List<Document>> retrieveAndFuse(String query, int topK, double similarityThreshold) {
        log.info("Начало этапа гибридного Retrieval для запроса: '{}'", query);

        // Шаг 1: Параллельный запуск поисковых запросов

        // Асинхронный векторный поиск
        CompletableFuture<List<Document>> vectorSearchFuture = CompletableFuture.supplyAsync(() -> {
            SearchRequest request = SearchRequest.builder()
                    .query(query).topK(topK).similarityThreshold(similarityThreshold).build();
            return vectorSearchService.search(request);
        }, taskExecutor);

        // Асинхронный полнотекстовый поиск (пример)
        // В реальном проекте здесь будет вызов к вашему сервису полнотекстового поиска
        CompletableFuture<List<Document>> fullTextSearchFuture = CompletableFuture.supplyAsync(() -> {
            log.info("Выполняется полнотекстовый поиск (симуляция)...");
            // return fullTextSearchService.search(query, topK);
            return List.of(); // Заглушка
        }, taskExecutor);

        // Шаг 2: Ожидание завершения всех поисков и слияние результатов
        return CompletableFuture.allOf(vectorSearchFuture, fullTextSearchFuture)
                .thenApply(v -> {
                    List<Document> vectorResults = vectorSearchFuture.join();
                    List<Document> fullTextResults = fullTextSearchFuture.join();
                    log.debug("Получено {} док-ов из векторного поиска и {} из полнотекстового.",
                            vectorResults.size(), fullTextResults.size());

                    // Шаг 3: Слияние с помощью RRF
                    List<List<Document>> resultsToFuse = Stream.of(vectorResults, fullTextResults)
                            .filter(list -> list != null && !list.isEmpty())
                            .toList();

                    List<Document> finalDocuments = fusionService.reciprocalRankFusion(resultsToFuse);

                    // Шаг 4: Ограничение итогового результата (опционально)
                    return finalDocuments.stream().limit(topK).toList();
                });
    }
}
