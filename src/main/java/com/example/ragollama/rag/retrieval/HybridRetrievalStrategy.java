package com.example.ragollama.rag.retrieval;

import com.example.ragollama.monitoring.KnowledgeGapService;
import com.example.ragollama.rag.agent.ProcessedQueries;
import com.example.ragollama.rag.retrieval.fusion.FusionService;
import com.example.ragollama.rag.retrieval.search.FtsSearchService;
import com.example.ragollama.rag.retrieval.search.VectorSearchService;
import com.example.ragollama.shared.metrics.MetricService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Реализует адаптивную гибридную стратегию извлечения документов.
 * <p>
 * Эта версия реализует двухэтапный поиск для оптимизации производительности:
 * <ol>
 *   <li>Сначала выполняется только точный векторный поиск по основному запросу.</li>
 *   <li>Если результатов недостаточно (меньше порога `expansionMinDocsThreshold`),
 *   запускается второй, более широкий этап поиска (FTS, expansion queries),
 *   и результаты сливаются с помощью Reciprocal Rank Fusion.</li>
 * </ol>
 * Это позволяет быстро отвечать на простые запросы, экономя ресурсы.
 * Все операции по извлечению данных являются полностью асинхронными и неблокирующими.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalStrategy {

    private final VectorSearchService vectorSearchService;
    private final FtsSearchService ftsSearchService;
    private final FusionService fusionService;
    private final RetrievalProperties retrievalProperties;
    private final MetricService metricService;
    private final KnowledgeGapService knowledgeGapService;

    /**
     * Асинхронно извлекает и сливает документы, применяя адаптивную логику.
     *
     * @param processedQueries    Объект с основным и расширенными запросами.
     * @param originalQuery       Оригинальный запрос пользователя для FTS.
     * @param topK                Количество документов для извлечения.
     * @param similarityThreshold Порог схожести.
     * @param businessFilter      Опциональный бизнес-фильтр (например, по типу документа).
     * @return {@link Mono} с финальным списком релевантных документов.
     */
    public Mono<List<Document>> retrieve(
            ProcessedQueries processedQueries,
            String originalQuery,
            int topK,
            double similarityThreshold,
            @Nullable Filter.Expression businessFilter) {

        if (processedQueries == null || processedQueries.primaryQuery().isBlank()) {
            knowledgeGapService.recordGap(originalQuery);
            return Mono.just(List.of());
        }
        int expansionThreshold = retrievalProperties.hybrid().expansionMinDocsThreshold();
        // Этап 1: Выполняем только точный (primary) векторный поиск.
        return executeVectorSearch(List.of(processedQueries.primaryQuery()), topK, similarityThreshold, businessFilter)
                .flatMap(primaryResults -> {
                    metricService.recordRetrievedDocumentsCount(primaryResults.size());

                    // Этап 2: Проверяем, достаточно ли результатов.
                    if (primaryResults.size() >= expansionThreshold) {
                        log.info("Адаптивный поиск: найдено достаточно ({}) документов на первом этапе. Пропуск расширенного поиска.", primaryResults.size());
                        return Mono.just(primaryResults);
                    }

                    log.info("Адаптивный поиск: найдено мало ({}) документов. Запуск расширенного поиска (FTS + Expansion).", primaryResults.size());

                    // Запускаем параллельно вторую волну поиска
                    Mono<List<Document>> expansionVectorSearch = executeVectorSearch(processedQueries.expansionQueries(), topK, similarityThreshold, businessFilter);
                    Mono<List<Document>> ftsSearch = ftsSearchService.search(originalQuery);

                    return Mono.zip(expansionVectorSearch, ftsSearch)
                            .map(tuple -> {
                                // Сливаем результаты первой и второй волны
                                List<List<Document>> allResults = List.of(primaryResults, tuple.getT1(), tuple.getT2());
                                return fusionService.reciprocalRankFusion(allResults);
                            });
                });
    }

    private Mono<List<Document>> executeVectorSearch(List<String> queries, int topK, double similarityThreshold, @Nullable Filter.Expression filter) {
        if (queries == null || queries.isEmpty()) {
            return Mono.just(List.of());
        }
        return vectorSearchService.search(queries, topK, similarityThreshold, filter);
    }
}
