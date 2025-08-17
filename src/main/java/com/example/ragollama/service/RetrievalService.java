package com.example.ragollama.service;

import com.example.ragollama.repository.DocumentFtsRepository;
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
 * <p>
 * Эта версия реализует <b>оптимизированный гибридный поиск</b>. Она принимает
 * два варианта запроса — семантический и лексический — и выполняет
 * соответствующие поиски параллельно, используя специализированный пул потоков.
 * <p>
 * Результаты обоих поисков объединяются с помощью {@link FusionService} для
 * получения единого, более релевантного списка документов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final VectorSearchService vectorSearchService;
    private final DocumentFtsRepository ftsRepository;
    private final FusionService fusionService;
    private final AsyncTaskExecutor applicationTaskExecutor;

    /**
     * Асинхронно выполняет гибридный поиск документов, используя разные запросы для семантического и лексического поиска.
     *
     * @param semanticQuery       Запрос, оптимизированный для семантического (векторного) поиска.
     * @param lexicalQuery        Оригинальный, немодифицированный запрос для лексического (FTS) поиска.
     * @param topK                Максимальное количество документов для извлечения из каждого источника.
     * @param similarityThreshold Минимальный порог схожести для векторного поиска.
     * @return {@link Mono}, который по завершении эммитит единый,
     *         переранжированный список найденных документов {@link Document}.
     */
    public Mono<List<Document>> retrieveDocuments(String semanticQuery, String lexicalQuery, int topK, double similarityThreshold) {
        log.info("Начало этапа гибридного Retrieval. Семантический: '{}', Лексический: '{}'", semanticQuery, lexicalQuery);

        // 1. Асинхронный вызов семантического поиска
        SearchRequest vectorRequest = SearchRequest.builder()
                .query(semanticQuery)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
        Mono<List<Document>> vectorSearchMono = Mono.fromCallable(() -> vectorSearchService.search(vectorRequest))
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));

        // 2. Асинхронный вызов лексического (FTS) поиска
        Mono<List<Document>> ftsSearchMono = Mono.fromCallable(() -> ftsRepository.searchByKeywords(lexicalQuery, topK))
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));

        // 3. Параллельное выполнение и слияние результатов
        return Mono.zip(vectorSearchMono, ftsSearchMono)
                .map(tuple -> {
                    List<Document> vectorResults = tuple.getT1();
                    List<Document> ftsResults = tuple.getT2();
                    log.debug("Получено {} док-ов от векторного поиска и {} от FTS.", vectorResults.size(), ftsResults.size());
                    return fusionService.reciprocalRankFusion(List.of(vectorResults, ftsResults));
                })
                .doOnSuccess(finalList -> log.info("Гибридный поиск завершен. Итоговый список содержит {} документов.", finalList.size()));
    }
}
