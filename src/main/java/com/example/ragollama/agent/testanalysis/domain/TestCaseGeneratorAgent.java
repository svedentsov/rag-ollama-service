package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.testanalysis.model.TestCase;
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
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который генерирует структурированные тест-кейсы
 * на основе текстового описания требований.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestCaseGeneratorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "test-case-generator";
    }

    @Override
    public String getDescription() {
        return "Генерирует структурированные тест-кейсы на основе текстового описания требований.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("requirementsText");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String requirementsText = (String) context.payload().get("requirementsText");
        log.info("TestCaseGeneratorAgent: запуск генерации для требований.");

        String promptString = promptService.render("testCaseGenerationPrompt", Map.of("requirements", requirementsText));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .thenApply(this::parseLlmResponse)
                .thenApply(testCases -> {
                    String summary = String.format("Генерация завершена. Создано %d тест-кейсов.", testCases.size());
                    log.info(summary);
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of("testCases", testCases)
                    );
                });
    }

    private List<TestCase> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            if (cleanedJson.isEmpty()) {
                log.warn("Не удалось извлечь JSON из ответа LLM. Ответ: {}", jsonResponse);
                return Collections.emptyList();
            }
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM для TestCase: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для TestCase.", e);
        }
    }
}
