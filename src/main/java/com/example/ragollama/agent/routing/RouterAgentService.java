package com.example.ragollama.agent.routing;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Сервис, реализующий логику "Router Agent".
 * <p>
 * Его единственная задача — быстро классифицировать запрос пользователя,
 * определив его намерение (intent). Для этого используется легковесная LLM
 * и специализированный промпт, что обеспечивает минимальную задержку.
 * Результат работы этого сервиса используется {@link com.example.ragollama.orchestration.OrchestrationService}
 * для выбора правильного конвейера обработки.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouterAgentService {

    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * Асинхронно определяет намерение пользователя на основе его запроса.
     *
     * @param query Запрос пользователя.
     * @return {@link Mono}, который по завершении будет содержать определенный {@link QueryIntent}.
     */
    public Mono<QueryIntent> route(String query) {
        String promptString = promptService.render("routerAgent", Map.of("query", query));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt, ModelCapability.FAST))
                .map(response -> parseIntentFromLlmResponse(response, query))
                .doOnSuccess(intent -> log.info("Запрос '{}' классифицирован с намерением: {}", query, intent));
    }

    /**
     * Безопасно парсит строковый ответ от LLM в {@link QueryIntent} с улучшенной fallback-логикой.
     *
     * @param response      Ответ от LLM.
     * @param originalQuery Оригинальный запрос пользователя для анализа в fallback-сценарии.
     * @return Распознанный {@link QueryIntent} или наиболее вероятный fallback.
     */
    private QueryIntent parseIntentFromLlmResponse(String response, String originalQuery) {
        if (response == null || response.isBlank()) {
            log.warn("RouterAgent получил пустой ответ от LLM. Используется fallback.");
            return fallbackToRagIfQuestion(originalQuery);
        }
        String cleanedResponse = response.trim().toUpperCase().replaceAll("[^A-Z_]", "");
        try {
            return QueryIntent.valueOf(cleanedResponse);
        } catch (IllegalArgumentException e) {
            log.warn("RouterAgent не смог распознать намерение из ответа LLM: '{}'. Используется fallback.", response);
            return fallbackToRagIfQuestion(originalQuery);
        }
    }

    /**
     * Fallback-стратегия: если запрос похож на вопрос, считаем его RAG_QUERY.
     *
     * @param query Оригинальный запрос.
     * @return {@link QueryIntent#RAG_QUERY} или {@link QueryIntent#UNKNOWN}.
     */
    private QueryIntent fallbackToRagIfQuestion(String query) {
        String lowerCaseQuery = query.toLowerCase();
        if (lowerCaseQuery.matches(".*(что|где|когда|кто|как|почему|сколько|какой|whose|what|where|when|who|how|why).*") || lowerCaseQuery.endsWith("?")) {
            log.debug("Fallback: запрос похож на вопрос, классифицируем как RAG_QUERY.");
            return QueryIntent.RAG_QUERY;
        }
        return QueryIntent.UNKNOWN;
    }
}
