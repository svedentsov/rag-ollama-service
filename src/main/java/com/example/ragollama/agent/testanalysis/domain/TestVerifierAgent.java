package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.testanalysis.model.TestValidationResult;
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
 * QA-агент, выполняющий роль ассистента-ревьюера для автотестов.
 * <p>
 * Анализирует предоставленный код теста и оценивает его качество
 * на основе предопределенных критериев, возвращая структурированную обратную связь.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestVerifierAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-verifier";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует и оценивает качество кода автотеста на Java.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("testCode");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String testCode = (String) context.payload().get("testCode");
        log.info("TestVerifierAgent: запуск анализа для предоставленного кода теста.");

        String promptString = promptService.render("testVerifierPrompt", Map.of("testCode", testCode));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(this::parseLlmResponse)
                .map(validationResult -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        validationResult.summary(),
                        Map.of("validationResult", validationResult)
                ));
    }

    private TestValidationResult parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            if (cleanedJson.isEmpty()) {
                throw new ProcessingException("LLM не вернула валидный JSON-блок.");
            }
            return objectMapper.readValue(cleanedJson, TestValidationResult.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM для TestValidationResult: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для TestValidationResult.", e);
        }
    }
}
