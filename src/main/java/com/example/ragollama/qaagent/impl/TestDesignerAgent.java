package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.GeneratedTestCase;
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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String requirements = (String) context.payload().get("requirementsText");
        String promptString = promptService.render("testDesigner", Map.of("requirements", requirements));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .thenApply(this::parseLlmResponse)
                .thenApply(testCase -> {
                    log.info("TestDesignerAgent успешно сгенерировал позитивный тест: {}", testCase.testName());
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Позитивный тест успешно сгенерирован.",
                            Map.of("initialTestCase", testCase) // Передаем результат в контекст для следующего агента
                    );
                });
    }

    private GeneratedTestCase parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, GeneratedTestCase.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("TestDesignerAgent LLM вернул невалидный JSON.", e);
        }
    }
}
