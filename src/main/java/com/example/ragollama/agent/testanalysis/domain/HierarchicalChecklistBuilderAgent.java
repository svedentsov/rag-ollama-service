package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.testanalysis.model.HierarchicalTestPlan;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Продвинутый AI-агент, который строит иерархические, контекстно-богатые
 * планы тестирования (чек-листы).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HierarchicalChecklistBuilderAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "hierarchical-checklist-builder";
    }

    @Override
    public String getDescription() {
        return "Строит иерархический, контекстно-богатый план тестирования.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("goal") && context.payload().containsKey("analysis_results_json");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String goal = (String) context.payload().get("goal");
        String analysisJson = (String) context.payload().get("analysis_results_json");

        String promptString = promptService.render("hierarchicalChecklistBuilderPrompt", Map.of(
                "goal", goal,
                "analysis_results_json", analysisJson
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                .map(tuple -> parseLlmResponse(tuple.getT1()))
                .map(testPlan -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Иерархический план тестирования успешно сгенерирован.",
                        Map.of("testPlan", testPlan)
                ));
    }

    private HierarchicalTestPlan parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, HierarchicalTestPlan.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Hierarchical Checklist Builder LLM вернул невалидный JSON.", e);
        }
    }
}
