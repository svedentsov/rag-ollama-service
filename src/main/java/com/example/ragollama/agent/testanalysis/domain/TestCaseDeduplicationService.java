package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.testanalysis.model.DeduplicationResult;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Сервис-ядро, реализующий двухступенчатый конвейер для обнаружения дубликатов тест-кейсов.
 * <p>
 * Сначала использует быстрый семантический поиск для нахождения кандидатов,
 * а затем выполняет точную верификацию каждой пары с помощью LLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestCaseDeduplicationService {

    private final HybridRetrievalStrategy retrievalStrategy;
    private final QueryProcessingPipeline queryProcessingPipeline;
    private final RetrievalProperties retrievalProperties;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * DTO для внутреннего использования при парсинге ответа LLM.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VerificationResponse(boolean isDuplicate, String justification) {
    }

    /**
     * Асинхронно находит дубликаты для предоставленного текста тест-кейса.
     *
     * @param sourceId      Уникальный идентификатор исходного теста (для исключения из поиска).
     * @param sourceContent Текст исходного тест-кейса для анализа.
     * @return {@link Mono} со списком подтвержденных дубликатов {@link DeduplicationResult}.
     */
    public Mono<List<DeduplicationResult>> findDuplicates(String sourceId, String sourceContent) {
        log.info("Запуск поиска дубликатов для тест-кейса ID: {}", sourceId);

        // Шаг 1: Найти кандидатов с помощью векторного поиска
        return findSimilarTestCases(sourceId, sourceContent)
                .flatMapMany(Flux::fromIterable)
                // Шаг 2: Для каждого кандидата асинхронно выполнить LLM-проверку
                .flatMap(candidate -> verifyIsDuplicate(sourceContent, candidate))
                .filter(Objects::nonNull) // Отфильтровать пары, которые не являются дубликатами
                .collectList();
    }

    /**
     * Использует RAG-конвейер для поиска семантически похожих тест-кейсов.
     *
     * @param sourceId ID исходного теста, который нужно исключить из результатов.
     * @param content  Текст исходного теста.
     * @return {@link Mono} со списком документов-кандидатов.
     */
    private Mono<List<Document>> findSimilarTestCases(String sourceId, String content) {
        Filter.Expression typeFilter = new Filter.Expression(
                Filter.ExpressionType.EQ, new Filter.Key("metadata.doc_type"), new Filter.Value("test_case")
        );
        Filter.Expression selfExcludeFilter = new Filter.Expression(
                Filter.ExpressionType.NE, new Filter.Key("metadata.documentId"), new Filter.Value(sourceId)
        );
        Filter.Expression combinedFilter = new Filter.Expression(Filter.ExpressionType.AND, typeFilter, selfExcludeFilter);

        var config = retrievalProperties.hybrid().vectorSearch();

        return queryProcessingPipeline.process(content)
                .flatMap(queries -> retrievalStrategy.retrieve(queries, content, config.topK() + 1, config.similarityThreshold(), combinedFilter));
    }

    /**
     * Вызывает LLM для окончательного вердикта по паре тест-кейсов.
     *
     * @param sourceContent Текст исходного теста.
     * @param candidate     Документ-кандидат.
     * @return {@link Mono} с результатом {@link DeduplicationResult}, если это дубликат, или пустой Mono.
     */
    private Mono<DeduplicationResult> verifyIsDuplicate(String sourceContent, Document candidate) {
        String promptString = promptService.render("testCaseDeduplicationPrompt", Map.of(
                "test_case_A", sourceContent,
                "test_case_B", candidate.getText()
        ));

        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(this::parseLlmResponse)
                .flatMap(verification -> {
                    if (verification.isDuplicate()) {
                        Float similarity = candidate.getMetadata().containsKey("distance")
                                ? 1 - (Float) candidate.getMetadata().get("distance")
                                : null;
                        String duplicateSourceId = (String) candidate.getMetadata().get("documentId");

                        return Mono.just(new DeduplicationResult(duplicateSourceId, verification.justification(), similarity));
                    }
                    return Mono.empty(); // Не является дубликатом
                });
    }

    /**
     * Безопасно парсит JSON-ответ от LLM.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Распарсенный объект VerificationResponse.
     * @throws ProcessingException если парсинг JSON не удался.
     */
    private VerificationResponse parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, VerificationResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM-верификатора дубликатов: {}", jsonResponse, e);
            throw new ProcessingException("LLM-верификатор вернул невалидный JSON.", e);
        }
    }
}
