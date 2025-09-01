package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
public class ChartGeneratorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "chart-generator-agent";
    }

    @Override
    public String getDescription() {
        return "Генерирует код диаграммы (Mermaid.js) на основе агрегированных данных.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("summarizedData") && context.payload().containsKey("chartType");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        JsonNode summarizedData = (JsonNode) context.payload().get("summarizedData");
        String chartType = (String) context.payload().get("chartType");

        try {
            String dataJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summarizedData);
            String promptString = promptService.render("chartGenerator", Map.of(
                    "chart_type", chartType,
                    "data_json", dataJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(mermaidCode -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Код для диаграммы Mermaid.js успешно сгенерирован.",
                            Map.of("visualizationCode", mermaidCode, "language", "mermaid")
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации агрегированных данных.", e));
        }
    }
}
