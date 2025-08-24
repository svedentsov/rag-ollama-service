package com.example.ragollama.qaagent.dynamic;

import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI-агент, отвечающий за планирование.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanningAgentService {

    private final LlmClient llmClient;
    private final ToolRegistryService toolRegistry;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    // Permissive mapper used as fallback
    private final ObjectMapper permissiveMapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);

    /**
     * Создает план выполнения на основе задачи пользователя.
     */
    public Mono<List<PlanStep>> createPlan(String taskDescription, Map<String, Object> initialContext) {
        String availableToolsJson = toolRegistry.getToolDescriptionsAsJson();
        String contextAsJson;
        try {
            contextAsJson = objectMapper.writeValueAsString(initialContext);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать initialContext в JSON", e);
            return Mono.error(new ProcessingException("Ошибка подготовки контекста для планировщика.", e));
        }

        String promptString = promptService.render("planningAgent", Map.of(
                "task", taskDescription,
                "tools", availableToolsJson,
                "context", contextAsJson
        ));
        log.info("Запрос к LLM-планировщику для задачи: '{}'", taskDescription);

        // Получаем ответ LLM, пытаемся распарсить; если не получилось — делаем 1 повторный запрос с явной инструкцией "ONLY JSON".
        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .flatMap(rawResponse -> tryParseOrReask(rawResponse, promptString))
                .doOnSuccess(plan -> log.info("LLM-планировщик сгенерировал план из {} шагов.", plan.size()));
    }

    /**
     * Попытка парсинга; при неудаче — один re-ask к LLM с требованием вернуть ТОЛЬКО JSON.
     */
    private Mono<List<PlanStep>> tryParseOrReask(String rawResponse, String originalPrompt) {
        // 1. Попытка сразу извлечь JSON и распарсить
        List<PlanStep> parsed = tryParse(rawResponse);
        if (parsed != null) {
            return Mono.just(parsed);
        }

        // 2. Сохраним оригинал (для отладки) и сформируем перезапрос
        log.warn("Первичный парсинг LLM-плана не удался. Попытка повторного запроса (re-ask). Отрывок ответа: {}",
                StringUtils.left(rawResponse, 800));
        saveLlMResponseForDebug(null, rawResponse); // реализуйте persistence по вашему усмотрению

        String reaskPrompt = buildReaskPrompt(originalPrompt, rawResponse);
        // Делать ещё один запрос и пытаться распарсить вновь
        return Mono.fromFuture(llmClient.callChat(new Prompt(reaskPrompt), ModelCapability.FAST))
                .map(this::tryParseOrThrow)
                .timeout(Duration.ofSeconds(20)) // разумный таймаут
                .onErrorMap(throwable -> {
                    log.error("Re-ask к LLM не помог: {}", throwable.getMessage(), throwable);
                    return new ProcessingException("LLM вернула невалидный JSON-план после повторного запроса.", throwable);
                });
    }

    /**
     * Возвращает Plan если получилось распарсить, иначе null (не кидаем здесь исключение).
     */
    private List<PlanStep> tryParse(String rawResponse) {
        try {
            String extracted = JsonExtractorUtil.extractJsonBlock(rawResponse);
            if (extracted.isBlank()) {
                return null;
            }

            // 1) строгий парсинг
            try {
                return objectMapper.readValue(extracted, new TypeReference<>() {
                });
            } catch (Exception strictEx) {
                // 2) permissive fallback
                try {
                    List<PlanStep> plan = permissiveMapper.readValue(extracted, new TypeReference<>() {
                    });
                    log.warn("Parsed plan with permissive settings (LLM returned slightly non-standard JSON).");
                    return plan;
                } catch (Exception permEx) {
                    log.debug("Both strict and permissive parsing failed. Extracted JSON (truncated): {}", StringUtils.left(extracted, 2000));
                    return null;
                }
            }
        } catch (Exception ex) {
            log.error("Ошибка при попытке извлечь JSON из ответа LLM", ex);
            return null;
        }
    }

    /**
     * Вспомогательный метод: парсим и кидаем ProcessingException, используется после re-ask.
     */
    private List<PlanStep> tryParseOrThrow(String rawResponse) {
        List<PlanStep> parsed = tryParse(rawResponse);
        if (parsed != null) return parsed;
        log.error("После повторного запроса LLM вернула невалидный JSON-план. Ответ (truncate 2000): {}", StringUtils.left(rawResponse, 2000));
        saveLlMResponseForDebug(null, rawResponse);
        throw new ProcessingException("LLM вернула невалидный JSON-план после повторного запроса.");
    }

    /**
     * Формирует короткий follow-up prompt, который просит LLM вернуть ТОЛЬКО JSON в нужной схеме.
     */
    private String buildReaskPrompt(String originalPrompt, String previousResponse) {
        // Обязательная часть: инструкция вернуть JSON строго по схеме PlanStep[]
        String instruction = """
                IMPORTANT: Return ONLY a valid JSON array (no explanation, no markdown fences, no extra text).
                The JSON must be an array of steps with fields: agentName (string) and arguments (object).
                Example:
                [
                  { "agentName": "git-inspector", "arguments": { "oldRef": "main", "newRef": "feature/x" } }
                ]
                """;

        return instruction + "\n\n" +
                "Previously I asked the following prompt:\n" + originalPrompt + "\n\n" +
                "The model returned (do not modify):\n" + previousResponse + "\n\n" +
                "Now: please return ONLY the JSON array with the plan.";
    }

    /**
     * Заглушка для сохранения полного LLM-ответа (для postmortem).
     * Реализуйте persist (S3 / DB / файл) в соответствии с архитектурой.
     */
    private void saveLlMResponseForDebug(String rid, String rawResponse) {
        // TODO: persist rawResponse вместе с rid/timestamp в durable хранилище
        log.info("Saving LLM response for debug (truncated 2000): {}", StringUtils.left(rawResponse, 2000));
    }
}
