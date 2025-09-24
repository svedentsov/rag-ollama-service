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
 * <p> Эта усовершенствованная версия реализует многоуровневую защиту:
 * <ol>
 *     <li><b>Быстрая блокировка:</b> Запросы, содержащие очевидные ключевые слова для атак, блокируются немедленно.</li>
 *     <li><b>Эвристический "Белый Список":</b> Быстрая детерминированная проверка на очевидно
 *         безопасные запросы (например, обычные вопросы) для экономии ресурсов.</li>
 *     <li><b>AI-анализ (Chain-of-Thought):</b> Для запросов, не прошедших быструю проверку,
 *         используется LLM, который сначала должен обосновать свое решение, а затем
 *         вынести вердикт. Это значительно снижает риск ложных срабатываний.</li>
 * </ol>
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
     * Поле `reasoning` имеет тип Object, чтобы гибко обрабатывать как строковые,
     * так и объектные ответы от LLM.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SecurityCheckResponse(boolean is_safe, Object reasoning) {
    }

    /**
     * Множество для быстрой проверки очевидно безопасных вопросительных слов.
     */
    private static final Set<String> WHITELISTED_KEYWORDS = Set.of(
            "что", "где", "когда", "кто", "как", "почему", "сколько", "какой", "расскажи", "опиши",
            "what", "where", "when", "who", "how", "why", "tell me", "describe", "explain"
    );

    private static final Set<String> BLACKLISTED_KEYWORDS = Set.of(
            "ignore previous", "ignore all", "забудь предыдущие", "игнорируй все", "system prompt",
            "act as", "ты теперь", "твои инструкции"
    );

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [01] Prompt Guard: проверка запроса на безопасность...");
        String query = context.originalQuery();
        // Уровень 1: Быстрая блокировка по "черному списку"
        if (isBlacklisted(query)) {
            log.warn("Обнаружен потенциально вредоносный ключ в запросе: '{}'. Запрос заблокирован.", query);
            return Mono.error(new PromptInjectionException("Обнаружена потенциально вредоносная инструкция.", query));
        }
        // Уровень 2: Быстрая эвристическая проверка по "белому списку"
        if (isWhitelisted(query)) {
            log.debug("Запрос '{}' прошел быструю эвристическую проверку (whitelist).", query);
            return Mono.just(context);
        }
        // Уровень 3: Глубокий анализ с помощью LLM
        log.debug("Запрос '{}' отправлен на глубокий AI-анализ безопасности.", query);
        String promptString = promptService.render("promptGuardPrompt", Map.of("query", query));
        Prompt prompt = new Prompt(promptString);
        return Mono.fromFuture(llmClient.callChat(prompt, ModelCapability.FASTEST, true))
                .flatMap(responseJson -> {
                    SecurityCheckResponse checkResponse = parseLlmResponse(responseJson);
                    if (!checkResponse.is_safe()) {
                        log.warn("Обнаружена и заблокирована потенциальная атака Prompt Injection. Запрос: '{}'. Обоснование AI: {}",
                                query, formatReasoning(checkResponse.reasoning()));
                        return Mono.error(new PromptInjectionException("Обнаружена потенциально вредоносная инструкция. Запрос отклонен.", query));
                    }
                    log.debug("AI-анализ безопасности для запроса пройден успешно. Обоснование: {}", formatReasoning(checkResponse.reasoning()));
                    return Mono.just(context);
                });
    }

    private boolean isBlacklisted(String query) {
        if (query == null) return false;
        String lowerCaseQuery = query.toLowerCase();
        return BLACKLISTED_KEYWORDS.stream().anyMatch(lowerCaseQuery::contains);
    }

    private boolean isWhitelisted(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String lowerCaseQuery = query.toLowerCase().trim();
        if (lowerCaseQuery.endsWith("?")) {
            return true;
        }
        return WHITELISTED_KEYWORDS.stream().anyMatch(lowerCaseQuery::startsWith);
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

    /**
     * Форматирует поле 'reasoning' для логирования,
     * корректно обрабатывая как строки, так и объекты.
     *
     * @param reasoning Поле из ответа LLM.
     * @return Отформатированная строка.
     */
    private String formatReasoning(Object reasoning) {
        if (reasoning instanceof String) {
            return (String) reasoning;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reasoning);
        } catch (JsonProcessingException e) {
            return String.valueOf(reasoning);
        }
    }
}
