package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.exception.LlmJsonResponseParseException;
import com.example.ragollama.shared.exception.PromptInjectionException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.task.TaskLifecycleService;
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
 * Шаг RAG-конвейера "Prompt Guard", отвечающий за проверку входящего запроса
 * на предмет атак типа "Prompt Injection".
 * <p>
 * Этот шаг выполняется самым первым и использует комбинацию эвристик и вызова
 * быстрой LLM для классификации запроса как безопасного или вредоносного.
 */
@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class PromptGuardStep implements RagPipelineStep {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final TaskLifecycleService taskLifecycleService;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * Внутренний DTO для парсинга JSON-ответа от LLM-классификатора.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SecurityCheckResponse(boolean is_safe, Object reasoning) {
    }

    /**
     * "Белый список" ключевых слов, указывающих на легитимный RAG-запрос.
     */
    private static final Set<String> WHITELISTED_KEYWORDS = Set.of(
            "что", "где", "когда", "кто", "как", "почему", "сколько", "какой", "расскажи", "опиши",
            "what", "where", "when", "who", "how", "why", "tell me", "describe", "explain"
    );

    /**
     * "Черный список" ключевых слов, часто используемых в атаках Prompt Injection.
     */
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

        // Отправляем статусное событие клиенту
        taskLifecycleService.getActiveTaskForSession(context.sessionId())
                .doOnNext(task -> taskLifecycleService.emitEvent(task.getId(), new UniversalResponse.StatusUpdate("Проверяю ваш вопрос на безопасность...")))
                .subscribe();

        String query = context.originalQuery();
        if (isBlacklisted(query)) {
            log.warn("Обнаружен потенциально вредоносный ключ в запросе: '{}'. Запрос заблокирован.", query);
            return Mono.error(new PromptInjectionException("Обнаружена потенциально вредоносная инструкция.", query));
        }
        if (isWhitelisted(query)) {
            log.debug("Запрос '{}' прошел быструю эвристическую проверку (whitelist).", query);
            return Mono.just(context);
        }
        log.debug("Запрос '{}' отправлен на глубокий AI-анализ безопасности.", query);
        String promptString = promptService.render("promptGuardPrompt", Map.of("query", query));
        Prompt prompt = new Prompt(promptString);
        return llmClient.callChat(prompt, ModelCapability.FASTEST, true)
                .flatMap(tuple -> {
                    SecurityCheckResponse checkResponse = parseLlmResponse(tuple.getT1());
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
        if (query == null || query.isBlank()) return true;
        String lowerCaseQuery = query.toLowerCase().trim();
        if (lowerCaseQuery.endsWith("?")) return true;
        return WHITELISTED_KEYWORDS.stream().anyMatch(lowerCaseQuery::startsWith);
    }

    private SecurityCheckResponse parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, SecurityCheckResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от PromptGuard LLM: {}", jsonResponse, e);
            throw new LlmJsonResponseParseException("PromptGuard LLM вернул невалидный JSON.", e, jsonResponse);
        }
    }

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
