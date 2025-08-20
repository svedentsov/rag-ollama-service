package com.example.ragollama.rag.retrieval;

import com.example.ragollama.rag.domain.reranking.RerankingService;
import com.example.ragollama.rag.domain.retrieval.DocumentFtsRepository;
import com.example.ragollama.rag.domain.retrieval.FusionService;
import com.example.ragollama.rag.domain.retrieval.VectorSearchService;
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
 * Гибридная стратегия извлечения — объединённый векторный + FTS + rerank.
 * Предположение по сигнатурам:
 * - vectorSearchService.search(SearchRequest) -> List<Document>
 * - ftsRepository.searchByKeywords(String, int) -> List<Document>
 * - retrievalProperties.hybrid().vectorSearch() имеет topK() и similarityThreshold()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalStrategy {

    private final VectorSearchService vectorSearchService;
    private final DocumentFtsRepository ftsRepository;
    private final FusionService fusionService;
    private final RerankingService rerankingService;
    private final RetrievalProperties retrievalProperties;
    private final AsyncTaskExecutor applicationTaskExecutor;

    public Mono<List<Document>> retrieve(List<String> queries, String originalQuery) {
        log.info("Запуск гибридной стратегии извлечения для {} запросов.", queries.size());

        var vectorSearchConfig = retrievalProperties.hybrid().vectorSearch();

        Mono<List<Document>> vectorSearchMono = Mono.fromCallable(() ->
                        queries.parallelStream()
                                .flatMap(query -> {
                                    SearchRequest request = SearchRequest.builder()
                                            .query(query) // <-- исправлено: query(...) вместо withQuery(...)
                                            .topK(vectorSearchConfig.topK())
                                            .similarityThreshold(vectorSearchConfig.similarityThreshold())
                                            .build();
                                    return vectorSearchService.search(request).stream();
                                })
                                .distinct()
                                .toList()
                )
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));

        var ftsConfig = retrievalProperties.hybrid().fts();
        Mono<List<Document>> ftsSearchMono = Mono.fromCallable(() -> ftsRepository.searchByKeywords(originalQuery, ftsConfig.topK()))
                .subscribeOn(Schedulers.fromExecutor(applicationTaskExecutor));

        return Mono.zip(vectorSearchMono, ftsSearchMono)
                .map(tuple -> {
                    List<Document> vectorResults = tuple.getT1();
                    List<Document> ftsResults = tuple.getT2();
                    log.debug("Получено {} док-ов от векторного поиска и {} от FTS.", vectorResults.size(), ftsResults.size());
                    return fusionService.reciprocalRankFusion(List.of(vectorResults, ftsResults));
                })
                .map(fusedDocs -> rerankingService.rerank(fusedDocs, originalQuery))
                .doOnSuccess(finalList -> log.info("Гибридный поиск завершен. Финальный список содержит {} документов.", finalList.size()));
    }
}
