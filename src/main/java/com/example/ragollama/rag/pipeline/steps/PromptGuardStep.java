package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.exception.LlmJsonResponseParseException;
import com.example.ragollama.shared.exception.PromptInjectionException;
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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

/**
 * Шаг RAG-конвейера, выполняющий роль "стража" на входе.
 *
 * <p>Эта усовершенствованная версия реализует многоуровневую защиту:
 * <ol>
 *     <li><b>Эвристический "Белый Список":</b> Быстрая детерминированная проверка на очевидно
 *         безопасные запросы (например, обычные вопросы) для экономии ресурсов.</li>
 *     <li><b>AI-анализ (Chain-of-Thought):</b> Для запросов, не прошедших быструю проверку,
 *         используется LLM, который сначала должен обосновать свое решение, а затем
 *         вынести вердикт. Это значительно снижает риск ложных срабатываний.</li>
 * </ol>
 *
 * <p>В случае обнаружения угрозы, выполнение конвейера прерывается с ошибкой
 * {@link PromptInjectionException}.
 */
@Component
@Order(1) // Наивысший приоритет, выполняется самым первым
@Slf4j
@RequiredArgsConstructor
public class PromptGuardStep implements RagPipelineStep {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * DTO для десериализации структурированного JSON-ответа от LLM-стража.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SecurityCheckResponse(boolean is_safe, String reasoning) {
    }

    /**
     * Множество для быстрой проверки очевидно безопасных вопросительных слов.
     */
    private static final Set<String> WHITELISTED_KEYWORDS = Set.of(
            "что", "где", "когда", "кто", "как", "почему", "сколько", "какой",
            "what", "where", "when", "who", "how", "why"
    );

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [01] Prompt Guard: проверка запроса на безопасность...");
        String query = context.originalQuery();
        // Уровень 1: Быстрая эвристическая проверка
        if (isWhitelisted(query)) {
            log.debug("Запрос '{}' прошел быструю эвристическую проверку (whitelist).", query);
            return Mono.just(context);
        }
        // Уровень 2: Глубокий анализ с помощью LLM
        log.debug("Запрос '{}' отправлен на глубокий AI-анализ безопасности.", query);
        String promptString = promptService.render("promptGuardPrompt", Map.of("query", query));
        Prompt prompt = new Prompt(promptString);
        return Mono.fromFuture(llmClient.callChat(prompt, ModelCapability.FASTEST, true))
                .flatMap(responseJson -> {
                    SecurityCheckResponse checkResponse = parseLlmResponse(responseJson);
                    if (!checkResponse.is_safe()) {
                        log.warn("Обнаружена и заблокирована потенциальная атака Prompt Injection. Запрос: '{}'. Обоснование AI: {}",
                                query, checkResponse.reasoning());
                        return Mono.error(new PromptInjectionException("Обнаружена потенциально вредоносная инструкция. Запрос отклонен."));
                    }
                    log.debug("AI-анализ безопасности для запроса пройден успешно. Обоснование: {}", checkResponse.reasoning());
                    return Mono.just(context);
                });
    }

    /**
     * Проверяет, является ли запрос очевидно безопасным вопросом.
     *
     * @param query Запрос пользователя.
     * @return {@code true}, если запрос безопасен, иначе {@code false}.
     */
    private boolean isWhitelisted(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String lowerCaseQuery = query.toLowerCase().trim();
        if (lowerCaseQuery.endsWith("?")) {
            return true;
        }
        String firstWord = lowerCaseQuery.split("\\s+")[0];
        return WHITELISTED_KEYWORDS.contains(firstWord);
    }

    /**
     * Безопасно парсит JSON-ответ от LLM.
     *
     * @param jsonResponse Ответ от LLM.
     * @return DTO {@link SecurityCheckResponse}.
     * @throws LlmJsonResponseParseException если парсинг не удался.
     */
    private SecurityCheckResponse parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, SecurityCheckResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от PromptGuard LLM: {}", jsonResponse, e);
            throw new LlmJsonResponseParseException("PromptGuard LLM вернул невалидный JSON.", e, jsonResponse);
        }
    }
}
