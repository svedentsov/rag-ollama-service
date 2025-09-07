package com.example.ragollama.agent.analytics.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.analytics.model.*;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI-агент, который строит интеллектуальную матрицу рисков.
 * <p>
 * Является "мета-агентом", который синтезирует результаты работы других
 * аналитических агентов для создания единой, приоритизированной картины рисков.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskMatrixGeneratorAgent implements ToolAgent {

    private final RiskScoringService riskScoringService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "risk-matrix-generator";
    }

    @Override
    public String getDescription() {
        return "Строит матрицу рисков (Вероятность x Влияние) для измененных файлов.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("qualityImpactReport") &&
                context.payload().containsKey("customerImpactReport");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        CodeQualityImpactReport qualityReport = (CodeQualityImpactReport) context.payload().get("qualityImpactReport");
        CustomerImpactReport customerImpactReport = (CustomerImpactReport) context.payload().get("customerImpactReport");

        Map<String, FileQualityImpact> qualityMap = qualityReport.riskProfiles().stream()
                .collect(Collectors.toMap(FileQualityImpact::filePath, Function.identity()));

        Map<String, CustomerImpactAnalysis> impactMap = customerImpactReport.analyses().stream()
                .collect(Collectors.toMap(analysis -> "TODO: extract file path", Function.identity())); // TODO: Need a way to link impact to file

        // Для простоты, сопоставим по первому файлу, но в идеале CustomerImpact должен содержать filePath
        List<RiskMatrixItem> riskItems = qualityMap.entrySet().stream()
                .map(entry -> {
                    String filePath = entry.getKey();
                    FileQualityImpact quality = entry.getValue();
                    CustomerImpactAnalysis impact = impactMap.values().stream().findFirst().orElse(null); // Simplified mapping

                    int likelihood = riskScoringService.calculateLikelihood(quality);
                    int impactScore = riskScoringService.calculateImpact(impact);

                    return new RiskMatrixItem(filePath, likelihood, impactScore, Map.of("quality", quality, "impact", impact));
                })
                .sorted((a, b) -> Integer.compare(b.likelihoodScore() * b.impactScore(), a.likelihoodScore() * a.impactScore()))
                .toList();

        return summarize(riskItems).thenApply(summary -> {
            RiskMatrixReport finalReport = new RiskMatrixReport(summary, riskItems);
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Матрица рисков успешно построена.", Map.of("riskMatrixReport", finalReport));
        });
    }

    private CompletableFuture<String> summarize(List<RiskMatrixItem> items) {
        if (items.isEmpty()) {
            return CompletableFuture.completedFuture("Изменений, требующих анализа, не найдено. Риски отсутствуют.");
        }
        try {
            String itemsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
            String promptString = promptService.render("riskMatrixSummaryPrompt", Map.of("matrixItemsJson", itemsJson));
            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED);
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации элементов матрицы риска", e));
        }
    }
}
