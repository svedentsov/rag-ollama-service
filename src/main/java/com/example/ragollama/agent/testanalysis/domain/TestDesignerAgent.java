package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.testanalysis.model.GeneratedTestCase;
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
 * Агент-"Строитель" в паре.
 * Его задача - сгенерировать качественный позитивный тест ("happy path")
 * на основе предоставленных требований.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestDesignerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "test-designer";
    }

    @Override
    public String getDescription() {
        return "Генерирует позитивный 'happy path' тест на основе требований.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("requirementsText");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String requirements = (String) context.payload().get("requirementsText");
        String promptString = promptService.render("testDesignerPrompt", Map.of("requirements", requirements));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                .map(tuple -> parseLlmResponse(tuple.getT1()))
                .map(testCase -> {
                    log.info("TestDesignerAgent успешно сгенерировал позитивный тест: {}", testCase.testName());
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Позитивный тест успешно сгенерирован.",
                            Map.of("initialTestCase", testCase)
                    );
                });
    }

    private GeneratedTestCase parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, GeneratedTestCase.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("TestDesignerAgent LLM вернул невалидный JSON.", e);
        }
    }
}
