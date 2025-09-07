package com.example.ragollama.agent.dynamic;

import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI-агент, отвечающий за планирование.
 * <p>Преобразует задачу на естественном языке в структурированный,
 * пошаговый план выполнения, используя доступные инструменты (других агентов).
 * Является stateless-компонентом, который выполняет чистую функцию
 * трансформации "запрос -> план".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanningAgentService {

    private final LlmClient llmClient;
    private final ToolRegistryService toolRegistry;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * Асинхронно создает план выполнения на основе задачи пользователя и начального контекста.
     *
     * @param taskDescription Задача на естественном языке (например, "Проанализируй изменения и найди риски").
     * @param initialContext  Начальные данные, которые могут помочь в планировании (например, Git-ссылки).
     * @return {@link Mono}, который по завершении будет содержать список шагов плана {@link PlanStep}.
     */
    public Mono<List<PlanStep>> createPlan(String taskDescription, Map<String, Object> initialContext) {
        String availableToolsJson = toolRegistry.getToolDescriptionsAsJson();
        String contextAsJson;
        try {
            contextAsJson = objectMapper.writeValueAsString(initialContext);
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка подготовки контекста для планировщика.", e));
        }

        String promptString = promptService.render("planningAgentPrompt", Map.of(
                "task", taskDescription,
                "tools", availableToolsJson,
                "context", contextAsJson
        ));
        log.info("Запрос к LLM-планировщику для задачи: '{}'", taskDescription);
        Prompt prompt = new Prompt(new UserMessage(promptString));

        return Mono.fromFuture(() -> llmClient.callChat(prompt, ModelCapability.BALANCED))
                .map(this::parsePlanFromLlmResponse)
                .doOnSuccess(plan -> log.info("LLM-планировщик сгенерировал план из {} шагов.", plan.size()))
                .doOnError(e -> log.error("Ошибка при создании плана для задачи '{}'", taskDescription, e));
    }

    /**
     * Надежно парсит JSON-ответ от LLM в список шагов плана.
     *
     * @param jsonResponse Сырой ответ от LLM, который может содержать markdown и другой "мусор".
     * @return Список объектов {@link PlanStep}.
     * @throws ProcessingException если парсинг не удался даже после очистки.
     */
    private List<PlanStep> parsePlanFromLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            if (cleanedJson.isEmpty() || "[]".equals(cleanedJson)) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-план от LLM: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON-план.", e);
        }
    }
}
