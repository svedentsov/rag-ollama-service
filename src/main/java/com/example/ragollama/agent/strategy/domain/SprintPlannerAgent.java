package com.example.ragollama.agent.strategy.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.analytics.model.BugPatternReport;
import com.example.ragollama.agent.strategy.model.SprintPlan;
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
 * Мета-агент, выступающий в роли "AI Product Manager".
 * <p>
 * Анализирует отчет о паттернах багов и формирует на его основе
 * приоритизированный план на следующий спринт.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SprintPlannerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "sprint-planner";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует паттерны багов и формирует план на спринт.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("bugPatternReport");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        BugPatternReport bugPatternReport = (BugPatternReport) context.payload().get("bugPatternReport");

        try {
            String reportJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bugPatternReport);
            String promptString = promptService.render("sprintPlannerPrompt", Map.of("bug_pattern_report_json", reportJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(plan -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            plan.sprintGoal(),
                            Map.of("sprintPlan", plan)
                    ));

        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчета о паттернах багов", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link SprintPlan}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link SprintPlan}.
     * @throws ProcessingException если парсинг не удался.
     */
    private SprintPlan parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, SprintPlan.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Sprint Planner LLM вернул невалидный JSON.", e);
        }
    }
}
