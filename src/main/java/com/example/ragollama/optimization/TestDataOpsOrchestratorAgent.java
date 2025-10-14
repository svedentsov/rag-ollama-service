package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.agent.dynamic.PlanStep;
import com.example.ragollama.agent.registry.ToolRegistryService;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Мета-агент (L4), выступающий в роли "Test DataOps Orchestrator".
 * <p>
 * Анализирует запрос на тестовые данные, с помощью LLM выбирает
 * подходящий инструмент-генератор и запускает его через
 * динамический исполнитель.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataOpsOrchestratorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<ToolRegistryService> toolRegistryProvider;
    private final DynamicPipelineExecutionService executionService;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-data-ops-orchestrator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует запрос на тестовые данные и оркестрирует их генерацию.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("goal");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String goal = (String) context.payload().get("goal");

        ToolRegistryService toolRegistry = toolRegistryProvider.getObject();
        String availableToolsJson = getFilteredToolDescriptions(toolRegistry);

        try {
            String contextJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("testDataOpsOrchestratorPrompt", Map.of(
                    "goal", goal,
                    "context", contextJson,
                    "tools", availableToolsJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                    .flatMap(tuple -> {
                        PlanStep stepToExecute = parseLlmResponse(tuple.getT1());
                        return executionService.executePlan(List.of(stepToExecute), context, null)
                                .map(results -> results.get(0));
                    });

        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации контекста для TestDataOps", e));
        }
    }

    private String getFilteredToolDescriptions(ToolRegistryService toolRegistry) {
        return toolRegistry.getToolDescriptionsAsJson();
    }

    private PlanStep parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ProcessingException("TestDataOps Orchestrator LLM вернул невалидный JSON.", e);
        }
    }
}
