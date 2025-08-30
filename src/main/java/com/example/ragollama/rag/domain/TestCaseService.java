package com.example.ragollama.rag.domain;

import com.example.ragollama.indexing.IndexingPipelineService;
import com.example.ragollama.indexing.IndexingRequest;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.api.dto.ManualIndexRequest;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Сервис-фасад, инкапсулирующий бизнес-логику для работы с тест-кейсами.
 * <p>
 * Включает как поиск (Finder), так и ручную индексацию (Indexer).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TestCaseService {

    private final QueryProcessingPipeline queryProcessingPipeline;
    private final HybridRetrievalStrategy retrievalStrategy;
    private final RetrievalProperties retrievalProperties;
    private final IndexingPipelineService indexingPipelineService;

    /**
     * Находит релевантные тест-кейсы на основе текстового запроса.
     *
     * @param query Запрос пользователя на естественном языке.
     * @return {@link Mono} со списком найденных документов, представляющих тест-кейсы.
     */
    public Mono<List<Document>> findRelevantTestCases(String query) {
        log.info("Поиск релевантных тест-кейсов для запроса: '{}'", query);
        // Создаем фильтр для поиска только документов с метаданными doc_type = 'test_case'
        Filter.Expression filter = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("metadata.doc_type"),
                new Filter.Value("test_case")
        );
        var retrievalConfig = retrievalProperties.hybrid().vectorSearch();
        return queryProcessingPipeline.process(query)
                .flatMap(processedQueries -> retrievalStrategy.retrieve(
                        processedQueries,
                        query,
                        retrievalConfig.topK(),
                        retrievalConfig.similarityThreshold(),
                        filter
                ));
    }

    /**
     * Выполняет on-demand индексацию одного тест-кейса.
     *
     * @param request DTO с данными для индексации.
     */
    public void indexManualTestCase(ManualIndexRequest request) {
        log.info("Запуск ручной индексации для тест-кейса: {}", request.filePath());
        IndexingRequest indexingRequest = new IndexingRequest(
                request.filePath(),
                request.filePath(),
                request.content(),
                Map.of("doc_type", "test_case")
        );
        indexingPipelineService.process(indexingRequest);
    }
}
