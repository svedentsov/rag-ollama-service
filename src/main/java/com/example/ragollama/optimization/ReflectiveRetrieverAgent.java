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

    public Mono<List<Document>> retrieve(ProcessedQueries queries, String originalQuery, int topK, double threshold, Filter.Expression filter) {
        return performRetrievalAttempt(queries, originalQuery, topK, threshold, filter, 1);
    }

    private Mono<List<Document>> performRetrievalAttempt(ProcessedQueries queries, String originalQuery, int topK, double threshold, Filter.Expression filter, int attempt) {
        if (attempt > MAX_ATTEMPTS) {
            log.warn("Достигнут лимит попыток самокоррекции для запроса: '{}'", originalQuery);
            // Возвращаем результат последнего успешного поиска
            return retrievalStrategy.retrieve(queries, originalQuery, topK, threshold, filter);
        }

        return retrievalStrategy.retrieve(queries, originalQuery, topK, threshold, filter)
                .flatMap(documents -> {
                    if (documents.isEmpty()) {
                        return Mono.just(documents);
                    }

                    return judgeRetrieval(originalQuery, documents)
                            .flatMap(judgeResult -> {
                                if (judgeResult.isSufficient()) {
                                    log.info("Оценка поиска (попытка {}): Документы признаны достаточными.", attempt);
                                    return Mono.just(documents);
                                } else {
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

    private Mono<JudgeResult> judgeRetrieval(String query, List<Document> documents) {
        // ИСПРАВЛЕНИЕ: Передаем в PromptService сам список документов, а не строку.
        String promptString = promptService.render("retrievalJudgePrompt", Map.of(
                "query", query,
                "documents", documents // <-- ИЗМЕНЕНИЕ
        ));

        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.FAST))
                .map(this::parseJudgeResponse);
    }

    private Mono<String> rewriteQuery(String originalQuery, String missingInfo) {
        String promptString = promptService.render("queryRewritePrompt", Map.of(
                "originalQuery", originalQuery,
                "missingInfo", missingInfo
        ));
        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.FAST));
    }

    private JudgeResult parseJudgeResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, JudgeResult.class);
        } catch (JsonProcessingException e) {
            log.error("LLM-судья вернул невалидный JSON: {}", jsonResponse, e);
            // В случае ошибки парсинга, считаем, что документы достаточны, чтобы не прерывать конвейер
            return new JudgeResult(true, "Ошибка парсинга ответа от AI-судьи.");
        }
    }
}
