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
import reactor.core.publisher.Mono;

import java.util.Map;

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
    private final JsonExtractorUtil jsonExtractorUtil;

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
        return context.payload().containsKey("testDebtReport") || context.payload().containsKey("bugPatternReport");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        try {
            String reportsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("refactoringStrategistPrompt", Map.of("health_reports_json", reportsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                    .map(tuple -> parseLlmResponse(tuple.getT1()))
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("refactoringReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации отчетов для Refactoring Strategist", e));
        }
    }

    private RefactoringReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, RefactoringReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Refactoring Strategist LLM вернул невалидный JSON.", e);
        }
    }
}
