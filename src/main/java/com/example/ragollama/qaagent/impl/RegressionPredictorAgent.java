package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.domain.HistoricalDefectService;
import com.example.ragollama.qaagent.model.FileCoverageRisk;
import com.example.ragollama.qaagent.model.RegressionRiskReport;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, прогнозирующий регрессионные риски на основе совокупности данных.
 * <p>
 * Этот агент синтезирует информацию из двух источников: текущее тестовое покрытие
 * и историческую стабильность затронутых файлов, чтобы предоставить
 * комплексную, приоритизированную оценку риска для каждого изменения.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegressionPredictorAgent implements ToolAgent {

    private final HistoricalDefectService historicalDefectService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "regression-predictor";
    }

    @Override
    public String getDescription() {
        return "Прогнозирует регрессионные риски, анализируя тестовое покрытие и историю дефектов.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("coverageRisks");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<FileCoverageRisk> coverageRisks = (List<FileCoverageRisk>) context.payload().get("coverageRisks");
        Map<String, Long> historicalFailures = historicalDefectService.getFailureCountsByClass(30);

        String dataForLlm;
        try {
            List<Map<String, Object>> riskProfiles = coverageRisks.stream()
                    .map(risk -> {
                        Map<String, Object> profile = new HashMap<>();
                        profile.put("filePath", risk.filePath());
                        profile.put("coveragePercentage", risk.coveragePercentage());
                        profile.put("historicalFailureCount", historicalFailures.getOrDefault(risk.filePath(), 0L));
                        return profile;
                    })
                    .toList();
            dataForLlm = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(riskProfiles);
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации данных для LLM", e));
        }

        String promptString = promptService.render("regressionPredictor", Map.of("riskDataJson", dataForLlm));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .thenApply(this::parseLlmResponse)
                .thenApply(report -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Прогноз регрессионных рисков успешно завершен.",
                        Map.of("regressionRiskReport", report)
                ));
    }

    private RegressionRiskReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, RegressionRiskReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON для отчета о рисках.", e);
        }
    }
}
