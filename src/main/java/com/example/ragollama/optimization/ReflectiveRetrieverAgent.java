package com.example.ragollama.optimization;

import com.example.ragollama.rag.agent.ProcessedQueries;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Агент, реализующий цикл самокорректирующегося поиска (Self-Correcting Retrieval).
 * Оценивает найденные документы и, при необходимости, переформулирует запрос для повторного поиска.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReflectiveRetrieverAgent {

    private final HybridRetrievalStrategy retrievalStrategy;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private static final int MAX_ATTEMPTS = 2;

    private record JudgeResult(boolean isSufficient, String reasoning) {
    }

    /**
     * Запускает итеративный процесс поиска.
     */
    public Mono<List<Document>> retrieve(ProcessedQueries queries, String originalQuery, int topK, double threshold, Filter.Expression filter) {
        return performRetrievalAttempt(queries, originalQuery, topK, threshold, filter, 1);
    }

    /**
     * Выполняет одну итерацию цикла "поиск -> оценка -> (опционально) переформулирование".
     */
    private Mono<List<Document>> performRetrievalAttempt(ProcessedQueries queries, String originalQuery, int topK, double threshold, Filter.Expression filter, int attempt) {
        if (attempt > MAX_ATTEMPTS) {
            log.warn("Достигнут лимит попыток самокоррекции для запроса: '{}'", originalQuery);
            return retrievalStrategy.retrieve(queries, originalQuery, topK, threshold, filter);
        }

        return retrievalStrategy.retrieve(queries, originalQuery, topK, threshold, filter)
                .flatMap(documents -> {
                    if (documents.isEmpty()) {
                        return Mono.just(documents); // Если ничего не найдено, нет смысла в оценке
                    }

                    // Шаг 2: Оценка найденных документов
                    return judgeRetrieval(originalQuery, documents)
                            .flatMap(judgeResult -> {
                                if (judgeResult.isSufficient()) {
                                    log.info("Оценка поиска (попытка {}): Документы признаны достаточными.", attempt);
                                    return Mono.just(documents);
                                } else {
                                    // Шаг 3: Переформулирование запроса и повторный поиск
                                    log.warn("Оценка поиска (попытка {}): Недостаточно информации. Причина: '{}'. Запуск переформулирования.", attempt, judgeResult.reasoning());
                                    return rewriteQuery(originalQuery, judgeResult.reasoning())
                                            .flatMap(newQuery -> {
                                                ProcessedQueries newQueries = new ProcessedQueries(newQuery, List.of());
                                                return performRetrievalAttempt(newQueries, originalQuery, topK, threshold, filter, attempt + 1);
                                            });
                                }
                            });
                });
    }

    /**
     * Вызывает LLM-судью для оценки релевантности найденных документов.
     */
    private Mono<JudgeResult> judgeRetrieval(String query, List<Document> documents) {
        String promptString = promptService.render("retrievalJudgePrompt", Map.of(
                "query", query,
                "documents", documents
        ));

        // Используем надежную модель для получения JSON
        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.FAST_RELIABLE, true))
                .map(this::parseJudgeResponse);
    }

    /**
     * Вызывает LLM для генерации нового поискового запроса.
     */
    private Mono<String> rewriteQuery(String originalQuery, String missingInfo) {
        String promptString = promptService.render("queryRewritePrompt", Map.of(
                "originalQuery", originalQuery,
                "missingInfo", missingInfo
        ));
        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.FASTEST));
    }

    private JudgeResult parseJudgeResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, JudgeResult.class);
        } catch (JsonProcessingException e) {
            log.error("LLM-судья вернул невалидный JSON: {}", jsonResponse, e);
            return new JudgeResult(true, "Ошибка парсинга ответа от AI-судьи.");
        }
    }
}
