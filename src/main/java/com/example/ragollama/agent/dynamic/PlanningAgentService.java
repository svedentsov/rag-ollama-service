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

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanningAgentService {

    private final LlmClient llmClient;
    private final ToolboxRegistryService toolboxRegistry;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    public Mono<List<PlanStep>> createPlan(String taskDescription, Map<String, Object> initialContext) {
        String toolboxesForPrompt = toolboxRegistry.getToolboxDescriptions().entrySet().stream()
                .map(e -> String.format("- %s: %s", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));

        String routerPrompt = promptService.render("agentRouterPrompt", Map.of(
                "task", taskDescription,
                "toolboxes", toolboxesForPrompt
        ));

        log.info("Запрос к AI-маршрутизатору для выбора Toolbox для задачи: '{}'", taskDescription);

        return Mono.fromFuture(() -> llmClient.callChat(new Prompt(routerPrompt), ModelCapability.FAST_RELIABLE))
                .flatMap(toolboxName -> {
                    String trimmedToolboxName = toolboxName.trim();
                    log.info("AI-маршрутизатор выбрал Toolbox: '{}'", trimmedToolboxName);

                    List<ToolAgent> tools = toolboxRegistry.getToolbox(trimmedToolboxName);
                    if (tools.isEmpty()) {
                        log.error("Маршрутизатор выбрал пустой или несуществующий Toolbox: '{}'.", trimmedToolboxName);
                        return Mono.error(new ProcessingException("Не удалось найти подходящие инструменты для выполнения задачи."));
                    }
                    return createPlanWithSelectedTools(taskDescription, initialContext, tools);
                });
    }

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

        return Mono.fromFuture(() -> llmClient.callChat(prompt, ModelCapability.BALANCED, true))
                .map(this::parsePlanFromLlmResponse)
                .doOnSuccess(plan -> log.info("LLM-планировщик сгенерировал план из {} шагов.", plan.size()))
                .doOnError(e -> log.error("Ошибка при создании плана для задачи '{}'", taskDescription, e));
    }

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
