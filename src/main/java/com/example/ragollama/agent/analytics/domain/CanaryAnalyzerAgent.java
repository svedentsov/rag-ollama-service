package com.example.ragollama.agent.analytics.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.analytics.api.dto.CanaryAnalysisRequest;
import com.example.ragollama.agent.analytics.model.CanaryAnalysisReport;
import com.example.ragollama.agent.analytics.model.MetricJudgement;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI-агент, который анализирует метрики канареечного развертывания.
 * <p>
 * Оркестрирует гибридный процесс: сначала вызывает детерминированный
 * {@link CanaryAnalysisService} для получения статистических результатов,
 * а затем передает их в LLM для интерпретации и вынесения вердикта.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanaryAnalyzerAgent implements ToolAgent {

    private final CanaryAnalysisService canaryAnalysisService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "canary-analyzer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует метрики baseline и canary, проводит статистический анализ и выносит Go/No-Go решение.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("metricsData");
    }

    /**
     * Асинхронно выполняет полный цикл канареечного анализа.
     *
     * @param context Контекст, содержащий метрики.
     * @return {@link CompletableFuture} с финальным отчетом.
     */
    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        Map<String, CanaryAnalysisRequest.MetricData> metricsData =
                (Map<String, CanaryAnalysisRequest.MetricData>) context.payload().get("metricsData");

        // Шаг 1: Детерминированный статистический анализ
        List<MetricJudgement> statisticalResults = canaryAnalysisService.performStatisticalAnalysis(metricsData);

        // Шаг 2: Вызов LLM для интерпретации и принятия решения
        try {
            String statsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statisticalResults);
            String promptString = promptService.render("canaryAnalysisPrompt", Map.of("statistical_results_json", statsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(llmResponse -> parseAndCombine(llmResponse, statisticalResults))
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("canaryReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации результатов статистики", e));
        }
    }

    /**
     * Парсит ответ от LLM и объединяет его с исходными статистическими данными.
     */
    private CanaryAnalysisReport parseAndCombine(String jsonResponse, List<MetricJudgement> statisticalResults) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            CanaryAnalysisReport llmReport = objectMapper.readValue(cleanedJson, CanaryAnalysisReport.class);

            Map<String, MetricJudgement> statsMap = statisticalResults.stream()
                    .collect(Collectors.toMap(MetricJudgement::metricName, Function.identity()));

            // Обогащаем интерпретациями от LLM
            List<MetricJudgement> finalJudgements = llmReport.metricJudgements().stream()
                    .map(llmJudgement -> {
                        MetricJudgement stats = statsMap.get(llmJudgement.metricName());
                        return new MetricJudgement(
                                stats.metricName(),
                                stats.statisticalResult(),
                                stats.pValue(),
                                llmJudgement.interpretation() // Берем интерпретацию от LLM
                        );
                    })
                    .toList();

            return new CanaryAnalysisReport(llmReport.overallDecision(), llmReport.executiveSummary(), finalJudgements);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM-аналитика: {}", jsonResponse, e);
            throw new ProcessingException("LLM-аналитик вернул невалидный JSON.", e);
        }
    }
}
