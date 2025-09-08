package com.example.ragollama.agent.dynamic;

import com.example.ragollama.agent.ToolAgent;
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
import java.util.stream.Collectors;

/**
 * AI-агент, отвечающий за планирование.
 * <p>
 * Эта версия реализует двухуровневую стратегию "Agent Squads" / "Toolboxes":
 * <ol>
 *   <li><b>Маршрутизация:</b> Сначала быстрый AI-агент-маршрутизатор выбирает наиболее
 *   подходящую группу инструментов ("Toolbox") для решения задачи.</li>
 *   <li><b>Планирование:</b> Затем основной AI-планировщик строит детальный план,
 *   используя ТОЛЬКО инструменты из выбранной группы.</li>
 * </ol>
 * Этот подход значительно повышает масштабируемость, качество и производительность
 * планирования по мере роста количества доступных инструментов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanningAgentService {

    private final LlmClient llmClient;
    private final ToolboxRegistryService toolboxRegistry;
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
        // === ЭТАП 1: МАРШРУТИЗАЦИЯ ===
        String toolboxesForPrompt = toolboxRegistry.getToolboxDescriptions().entrySet().stream()
                .map(e -> String.format("- %s: %s", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));

        // Примечание: Для этого шага нужен новый промпт 'agentRouterPrompt.ftl'
        String routerPrompt = promptService.render("agentRouterPrompt", Map.of(
                "task", taskDescription,
                "toolboxes", toolboxesForPrompt
        ));

        log.info("Запрос к AI-маршрутизатору для выбора Toolbox для задачи: '{}'", taskDescription);

        return Mono.fromFuture(() -> llmClient.callChat(new Prompt(routerPrompt), ModelCapability.FAST))
                .flatMap(toolboxName -> {
                    String trimmedToolboxName = toolboxName.trim();
                    log.info("AI-маршрутизатор выбрал Toolbox: '{}'", trimmedToolboxName);

                    List<ToolAgent> tools = toolboxRegistry.getToolbox(trimmedToolboxName);
                    if (tools.isEmpty()) {
                        log.error("Маршрутизатор выбрал пустой или несуществующий Toolbox: '{}'.", trimmedToolboxName);
                        return Mono.error(new ProcessingException("Не удалось найти подходящие инструменты для выполнения задачи."));
                    }

                    // === ЭТАП 2: ПЛАНИРОВАНИЕ ===
                    return createPlanWithSelectedTools(taskDescription, initialContext, tools);
                });
    }

    /**
     * Внутренний метод для выполнения второго этапа - планирования с ограниченным набором инструментов.
     */
    private Mono<List<PlanStep>> createPlanWithSelectedTools(String taskDescription, Map<String, Object> initialContext, List<ToolAgent> tools) {
        String availableToolsJson = getToolDescriptionsAsJson(tools);
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
        log.info("Запрос к LLM-планировщику (с {} инструментами) для задачи: '{}'", tools.size(), taskDescription);
        Prompt prompt = new Prompt(new UserMessage(promptString));

        return Mono.fromFuture(() -> llmClient.callChat(prompt, ModelCapability.BALANCED))
                .map(this::parsePlanFromLlmResponse)
                .doOnSuccess(plan -> log.info("LLM-планировщик сгенерировал план из {} шагов.", plan.size()))
                .doOnError(e -> log.error("Ошибка при создании плана для задачи '{}'", taskDescription, e));
    }

    /**
     * Форматирует описания предоставленных агентов-инструментов в JSON-строку.
     *
     * @param agents Список агентов для включения в описание.
     * @return JSON-строка.
     */
    private String getToolDescriptionsAsJson(List<ToolAgent> agents) {
        List<Map<String, String>> toolDescriptions = agents.stream()
                .map(agent -> Map.of(
                        "name", agent.getName(),
                        "description", agent.getDescription()
                ))
                .toList();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toolDescriptions);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать описания инструментов в JSON", e);
            return "[]";
        }
    }

    /**
     * Надежно парсит JSON-ответ от LLM в список шагов плана.
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
