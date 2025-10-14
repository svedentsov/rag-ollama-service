package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSummarizerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "data-summarizer-agent";
    }

    @Override
    public String getDescription() {
        return "Агрегирует сырые JSON-данные в простую структуру для построения графика.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("data") && context.payload().containsKey("instruction");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        JsonNode data = (JsonNode) context.payload().get("data");
        String instruction = (String) context.payload().get("instruction");

        try {
            String dataJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            String promptString = promptService.render("dataSummarizerPrompt", Map.of(
                    "data_json", dataJson,
                    "instruction", instruction
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                    .map(tuple -> parseLlmResponse(tuple.getT1()))
                    .map(summary -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Данные успешно агрегированы для визуализации.",
                            Map.of("summarizedData", summary)
                    ));

        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации данных для саммаризации.", e));
        }
    }

    private JsonNode parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readTree(cleanedJson);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("DataSummarizerAgent LLM вернул невалидный JSON.", e);
        }
    }
}
