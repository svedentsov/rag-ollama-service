package com.example.ragollama.optimization;

import com.example.ragollama.agent.dynamic.ToolRegistryService;
import com.example.ragollama.optimization.model.WorkflowNode;
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
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Продвинутый AI-агент-планировщик, который декомпозирует сложную задачу
 * в граф зависимостей (DAG), а не в простую линейную последовательность.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowPlannerAgent {

    private final LlmClient llmClient;
    private final ToolRegistryService toolRegistry;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * Асинхронно создает граф выполнения (DAG) на основе цели пользователя.
     *
     * @param goal    Задача на естественном языке.
     * @param context Начальные данные.
     * @return {@link Mono} со списком узлов {@link WorkflowNode}, представляющих граф.
     */
    public Mono<List<WorkflowNode>> createWorkflow(String goal, Map<String, Object> context) {
        String availableToolsJson = toolRegistry.getToolDescriptionsAsJson();
        String contextAsJson;
        try {
            contextAsJson = objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка подготовки контекста для Workflow-планировщика.", e));
        }

        String promptString = promptService.render("workflowPlannerPrompt", Map.of(
                "goal", goal,
                "tools", availableToolsJson,
                "context", contextAsJson
        ));
        log.info("Запрос к LLM-планировщику для построения workflow: '{}'", goal);

        return Mono.fromFuture(() -> llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(this::parseWorkflow)
                .doOnSuccess(plan -> log.info("LLM-планировщик сгенерировал workflow из {} узлов.", plan.size()));
    }

    private List<WorkflowNode> parseWorkflow(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-workflow от LLM: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON-workflow.", e);
        }
    }
}
