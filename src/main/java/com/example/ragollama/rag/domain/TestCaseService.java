package com.example.ragollama.rag.domain;

import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Сервис-фасад, инкапсулирующий логику поиска по тест-кейсам.
 * <p>
 * Реализует паттерн специализированного поиска: принимает пользовательский запрос,
 * формирует правильный фильтр по метаданным и делегирует выполнение
 * универсальной стратегии извлечения {@link HybridRetrievalStrategy}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TestCaseService {

    private final QueryProcessingPipeline queryProcessingPipeline;
    private final HybridRetrievalStrategy retrievalStrategy;
    private final RetrievalProperties retrievalProperties;

    /**
     * Находит релевантные тест-кейсы на основе текстового запроса.
     *
     * @param query Запрос пользователя на естественном языке.
     * @return {@link Mono} со списком найденных документов, представляющих тест-кейсы.
     */
    public Mono<List<Document>> findRelevantTestCases(String query) {
        log.info("Поиск релевантных тест-кейсов для запроса: '{}'", query);

        // Шаг 1: Создаем фильтр для поиска только документов с метаданными doc_type = 'test_case'
        Filter.Expression filter = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("metadata.doc_type"),
                new Filter.Value("test_case")
        );

        var retrievalConfig = retrievalProperties.hybrid().vectorSearch();

        // Шаг 2: Вызываем стандартный конвейер обработки запроса
        return queryProcessingPipeline.process(query)
                // Шаг 3: Вызываем универсальную стратегию извлечения, но с нашим кастомным фильтром
                .flatMap(processedQueries -> retrievalStrategy.retrieve(
                        processedQueries,
                        query,
                        retrievalConfig.topK(),
                        retrievalConfig.similarityThreshold(),
                        filter // Передаем фильтр
                ));
    }
}
