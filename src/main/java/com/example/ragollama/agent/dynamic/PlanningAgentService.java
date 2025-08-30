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

import java.util.List;
import java.util.Map;

/**
 * AI-агент, отвечающий за планирование.
 * <p>
 * Преобразует задачу на естественном языке в структурированный,
 * пошаговый план выполнения, используя доступные инструменты (других агентов).
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
     * Создает план выполнения на основе задачи пользователя.
     *
     * @param taskDescription Задача на естественном языке.
     * @param initialContext  Начальные данные, которые могут помочь в планировании.
     * @return {@link Mono} со списком шагов плана.
     */
    public Mono<List<PlanStep>> createPlan(String taskDescription, Map<String, Object> initialContext) {
        String availableToolsJson = toolRegistry.getToolDescriptionsAsJson();
        String contextAsJson;
        try {
            contextAsJson = objectMapper.writeValueAsString(initialContext);
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка подготовки контекста для планировщика.", e));
        }

        String promptString = promptService.render("planningAgent", Map.of(
                "task", taskDescription,
                "tools", availableToolsJson,
                "context", contextAsJson
        ));
        log.info("Запрос к LLM-планировщику для задачи: '{}'", taskDescription);
        Prompt prompt = new Prompt(new UserMessage(promptString));
        return Mono.fromFuture(llmClient.callChat(prompt, ModelCapability.BALANCED))
                .map(this::parsePlanFromLlmResponse)
                .doOnSuccess(plan -> log.info("LLM-планировщик сгенерировал план из {} шагов.", plan.size()));
    }

    private List<PlanStep> parsePlanFromLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-план от LLM: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON-план.", e);
        }
    }
}
