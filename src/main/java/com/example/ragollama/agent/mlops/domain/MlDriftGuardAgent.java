package com.example.ragollama.agent.mlops.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.mlops.model.DriftReport;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который анализирует и интерпретирует результаты статистического
 * анализа дрейфа признаков.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MlDriftGuardAgent implements ToolAgent {

    private final FeatureDriftAnalysisService analysisService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "ml-drift-guard";
    }

    @Override
    public String getDescription() {
        return "Интерпретирует статистический отчет о дрейфе признаков и выносит вердикт.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("baselineData") && context.payload().containsKey("productionData");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<Map<String, Object>> baselineData = (List<Map<String, Object>>) context.payload().get("baselineData");
        List<Map<String, Object>> productionData = (List<Map<String, Object>>) context.payload().get("productionData");

        // Шаг 1: Детерминированный статистический анализ
        return CompletableFuture.supplyAsync(() -> analysisService.analyze(baselineData, productionData))
                .thenCompose(statisticalResults -> {
                    if (statisticalResults.isEmpty()) {
                        return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Нет общих признаков для анализа дрейфа.", Map.of()));
                    }
                    // Шаг 2: Вызов LLM для интерпретации
                    try {
                        String statsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statisticalResults);
                        String promptString = promptService.render("mlDriftGuard", Map.of("statistical_report_json", statsJson));

                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .thenApply(this::parseLlmResponse)
                                .thenApply(report -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        report.executiveSummary(),
                                        Map.of("driftReport", report)
                                ));
                    } catch (JsonProcessingException e) {
                        return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчета о дрейфе", e));
                    }
                });
    }

    private DriftReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, DriftReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от ML Drift Guard LLM: {}", jsonResponse, e);
            throw new ProcessingException("ML Drift Guard LLM вернул невалидный JSON.", e);
        }
    }
}
