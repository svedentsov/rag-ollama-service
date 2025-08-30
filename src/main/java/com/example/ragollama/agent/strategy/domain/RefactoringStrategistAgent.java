package com.example.ragollama.agent.strategy.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.strategy.model.RefactoringReport;
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
 * Мета-агент, выступающий в роли "AI Tech Lead".
 * <p>
 * Агрегирует отчеты о техдолге, паттернах багов и производительности,
 * чтобы синтезировать высокоуровневую стратегию рефакторинга.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefactoringStrategistAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "refactoring-strategist";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует отчеты о здоровье репозитория и предлагает стратегические инициативы по рефакторингу.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        // Запускается как финальный шаг, если есть отчеты для анализа
        return context.payload().containsKey("testDebtReport") || context.payload().containsKey("bugPatternReport");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        try {
            String reportsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("refactoringStrategist", Map.of("health_reports_json", reportsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("refactoringReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчетов для Refactoring Strategist", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link RefactoringReport}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link RefactoringReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private RefactoringReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, RefactoringReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Refactoring Strategist LLM вернул невалидный JSON.", e);
        }
    }
}
