package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.qaagent.dynamic.PlanStep;
import com.example.ragollama.qaagent.model.CanaryAnalysisReport;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который оркестрирует принятие решений по канареечным развертываниям.
 * <p>
 * Принимает на вход отчет от CanaryAnalyzerAgent и политику, а затем генерирует
 * и запускает план действий (promote, rollback, hold).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanaryDecisionOrchestratorAgent implements ToolAgent {

    private final DynamicPipelineExecutionService executionService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "canary-decision-orchestrator";
    }

    @Override
    public String getDescription() {
        return "Принимает отчет о канареечном анализе и политику, а затем генерирует и выполняет план действий.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("canaryReport") && context.payload().containsKey("decisionPolicy");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        CanaryAnalysisReport report = (CanaryAnalysisReport) context.payload().get("canaryReport");
        String policy = (String) context.payload().get("decisionPolicy");

        try {
            String reportJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
            String promptString = promptService.render("canaryDecisionMaker", Map.of(
                    "canary_report_json", reportJson,
                    "decision_policy", policy
            ));

            // Шаг 1: LLM генерирует план действий
            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parsePlan)
                    .thenCompose(plan -> {
                        // Шаг 2: Выполняем сгенерированный план
                        log.info("Запуск сгенерированного Canary-плана с {} шагами.", plan.size());
                        return executionService.executePlan(plan, new AgentContext(Map.of()), null)
                                .toFuture()
                                .thenApply(results -> {
                                    String summary = "Оркестрация решения по canary-развертыванию завершена. План запущен.";
                                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("executedPlan", results));
                                });
                    });
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчета для оркестратора", e));
        }
    }

    private List<PlanStep> parsePlan(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-план от Canary Orchestrator LLM: {}", jsonResponse, e);
            throw new ProcessingException("Canary Orchestrator LLM вернул невалидный JSON-план.", e);
        }
    }
}
