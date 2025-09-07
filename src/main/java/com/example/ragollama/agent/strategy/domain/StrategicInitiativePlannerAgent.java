package com.example.ragollama.agent.strategy.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.strategy.model.FederatedReport;
import com.example.ragollama.agent.strategy.model.PortfolioStrategyReport;
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
 * Мета-агент, выступающий в роли "AI CTO" или "Executive Governor".
 * <p>
 * Принимает на вход аналитический отчет о здоровье всех проектов и
 * синтезирует из него высокоуровневый стратегический план с конкретными
 * инициативами и KPI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrategicInitiativePlannerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "strategic-initiative-planner";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует федеративный отчет и формирует стратегический план на квартал.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("federatedReport");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        FederatedReport federatedReport = (FederatedReport) context.payload().get("federatedReport");

        try {
            String reportJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(federatedReport);
            String promptString = promptService.render("strategicInitiativePlannerPrompt", Map.of("federated_report_json", reportJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(plan -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            plan.quarterlyGoal(),
                            Map.of("portfolioStrategyReport", plan)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации федеративного отчета", e));
        }
    }

    private PortfolioStrategyReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, PortfolioStrategyReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Strategic Initiative Planner LLM вернул невалидный JSON.", e);
        }
    }
}
