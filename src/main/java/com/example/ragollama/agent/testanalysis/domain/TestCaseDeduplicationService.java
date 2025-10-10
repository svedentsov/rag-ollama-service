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
    private final JsonExtractorUtil jsonExtractorUtil;

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

        return findSimilarTestCases(sourceId, sourceContent)
                .flatMapMany(Flux::fromIterable)
                .flatMap(candidate -> verifyIsDuplicate(sourceContent, candidate))
                .filter(Objects::nonNull)
                .collectList();
    }

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

    private Mono<DeduplicationResult> verifyIsDuplicate(String sourceContent, Document candidate) {
        String promptString = promptService.render("testCaseDeduplicationPrompt", Map.of(
                "test_case_A", sourceContent,
                "test_case_B", candidate.getText()
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(this::parseLlmResponse)
                .flatMap(verification -> {
                    if (verification.isDuplicate()) {
                        Float similarity = candidate.getMetadata().containsKey("distance")
                                ? 1 - (Float) candidate.getMetadata().get("distance")
                                : null;
                        String duplicateSourceId = (String) candidate.getMetadata().get("documentId");

                        return Mono.just(new DeduplicationResult(duplicateSourceId, verification.justification(), similarity));
                    }
                    return Mono.empty();
                });
    }

    private VerificationResponse parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, VerificationResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM-верификатора дубликатов: {}", jsonResponse, e);
            throw new ProcessingException("LLM-верификатор вернул невалидный JSON.", e);
        }
    }
}
