package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.testanalysis.model.TestRefactoringResult;
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
 * QA-агент, который анализирует код автотеста на предмет "запахов" (smells)
 * и предлагает улучшенную, отрефакторенную версию.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestSmellRefactorerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-smell-refactorer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует код автотеста на 'запахи' и предлагает отрефакторенную версию.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("testCode");
    }

    /**
     * Асинхронно выполняет анализ и рефакторинг кода.
     *
     * @param context Контекст, содержащий исходный код теста.
     * @return {@link CompletableFuture} с результатом, содержащим отчет о рефакторинге.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String testCode = (String) context.payload().get("testCode");
        log.info("TestSmellRefactorerAgent: запуск анализа и рефакторинга кода...");

        String promptString = promptService.render("testSmellRefactorerPrompt", Map.of("testCode", testCode));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .thenApply(this::parseLlmResponse)
                .thenApply(refactoringResult -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Рефакторинг кода успешно предложен. Найдено запахов: " + refactoringResult.smellsFound().size(),
                        Map.of("refactoringResult", refactoringResult)
                ));
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link TestRefactoringResult}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link TestRefactoringResult}.
     * @throws ProcessingException если парсинг не удался.
     */
    private TestRefactoringResult parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, TestRefactoringResult.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM для TestRefactoringResult: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для TestRefactoringResult.", e);
        }
    }
}
