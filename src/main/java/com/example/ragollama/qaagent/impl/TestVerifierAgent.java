package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.model.TestValidationResult;
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
 * QA-агент, выполняющий роль ассистента-ревьюера для автотестов.
 * <p>
 * Анализирует предоставленный код теста и оценивает его качество
 * на основе предопределенных критериев, возвращая структурированную обратную связь.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestVerifierAgent implements QaAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

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
     *
     * @param context Контекст, содержащий ключ 'testCode'.
     * @return {@code true} если контекст содержит код теста.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("testCode");
    }

    /**
     * {@inheritDoc}
     *
     * @param context Контекст с кодом теста.
     * @return {@link CompletableFuture} со структурированным результатом анализа.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String testCode = (String) context.payload().get("testCode");
        log.info("TestVerifierAgent: запуск анализа для предоставленного кода теста.");

        String promptString = promptService.render("testVerifier", Map.of("testCode", testCode));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .thenApply(this::parseLlmResponse)
                .thenApply(validationResult -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        validationResult.summary(),
                        Map.of("validationResult", validationResult)
                ));
    }

    /**
     * Безопасно парсит JSON-ответ от LLM.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link TestValidationResult}.
     * @throws ProcessingException если парсинг не удался.
     */
    private TestValidationResult parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
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
