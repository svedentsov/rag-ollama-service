package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimulationAnalysisAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "simulation-analysis-agent";
    }

    @Override
    public String getDescription() {
        return "Анализирует результаты симуляции и генерирует отчет с выводами.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("goal") && context.payload().size() > 1;
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String goal = (String) context.payload().get("goal");
        try {
            String resultsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("simulationAnalyzerPrompt", Map.of(
                    "goal", goal,
                    "simulation_results_json", resultsJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Анализ симуляции завершен.",
                            Map.of("simulationReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации результатов симуляции.", e));
        }
    }
}
