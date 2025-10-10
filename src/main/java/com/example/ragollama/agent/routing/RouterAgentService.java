package com.example.ragollama.agent.routing;

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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Сервис, реализующий логику "Router Agent".
 * Его задача — быстро и НАДЕЖНО классифицировать запрос пользователя,
 * определив его намерение (intent). Для этого используется быстрая, но
 * надежная LLM и промпт, требующий JSON-ответа.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouterAgentService {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record IntentResponse(QueryIntent intent) {
    }

    /**
     * Асинхронно определяет намерение пользователя на основе его запроса.
     *
     * @param query Запрос пользователя.
     * @return {@link Mono}, который по завершении будет содержать определенный {@link QueryIntent}.
     */
    public Mono<QueryIntent> route(String query) {
        String promptString = promptService.render("routerAgentPrompt", Map.of("query", query));
        Prompt prompt = new Prompt(promptString);
        return llmClient.callChat(prompt, ModelCapability.FAST_RELIABLE, true)
                .map(this::parseIntentFromLlmResponse)
                .doOnSuccess(intent -> log.info("Запрос '{}' классифицирован с намерением: {}", query, intent))
                .onErrorResume(e -> {
                    log.warn("Ошибка при маршрутизации запроса '{}'. Используется fallback.", query, e);
                    return Mono.just(fallbackLogic(query));
                });
    }

    private QueryIntent parseIntentFromLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            if (cleanedJson.isEmpty()) {
                log.warn("LLM-маршрутизатор вернул пустой JSON. Ответ: {}", jsonResponse);
                throw new ProcessingException("LLM-маршрутизатор вернул пустой JSON.");
            }
            IntentResponse response = objectMapper.readValue(cleanedJson, IntentResponse.class);
            return response.intent != null ? response.intent : QueryIntent.UNKNOWN;
        } catch (JsonProcessingException e) {
            log.error("LLM-маршрутизатор вернул невалидный JSON: {}", jsonResponse, e);
            throw new ProcessingException("LLM-маршрутизатор вернул невалидный JSON.", e);
        }
    }

    private QueryIntent fallbackLogic(String query) {
        String lowerCaseQuery = query.toLowerCase();
        if (lowerCaseQuery.matches(".*\\b(код|code|пример|example|sample|java|python|javascript|selenide)\\b.*")) {
            log.debug("Fallback: запрос содержит ключевые слова для кода, классифицируем как CODE_GENERATION.");
            return QueryIntent.CODE_GENERATION;
        }
        if (lowerCaseQuery.matches(".*(что|где|когда|кто|как|почему|сколько|какой|whose|what|where|when|who|how|why).*") || lowerCaseQuery.endsWith("?")) {
            log.debug("Fallback: запрос похож на вопрос, классифицируем как RAG_QUERY.");
            return QueryIntent.RAG_QUERY;
        }
        return QueryIntent.UNKNOWN;
    }
}
