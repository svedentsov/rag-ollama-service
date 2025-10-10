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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI-агент, который анализирует метрики канареечного развертывания.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanaryAnalyzerAgent implements ToolAgent {

    private final CanaryAnalysisService canaryAnalysisService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "canary-analyzer";
    }

    @Override
    public String getDescription() {
        return "Анализирует метрики baseline и canary, проводит статистический анализ и выносит Go/No-Go решение.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("metricsData");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        Map<String, CanaryAnalysisRequest.MetricData> metricsData =
                (Map<String, CanaryAnalysisRequest.MetricData>) context.payload().get("metricsData");

        List<MetricJudgement> statisticalResults = canaryAnalysisService.performStatisticalAnalysis(metricsData);

        try {
            String statsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statisticalResults);
            String promptString = promptService.render("canaryAnalysisPrompt", Map.of("statistical_results_json", statsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                    .map(llmResponse -> parseAndCombine(llmResponse, statisticalResults))
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("canaryReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации результатов статистики", e));
        }
    }

    private CanaryAnalysisReport parseAndCombine(String jsonResponse, List<MetricJudgement> statisticalResults) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            CanaryAnalysisReport llmReport = objectMapper.readValue(cleanedJson, CanaryAnalysisReport.class);

            Map<String, MetricJudgement> statsMap = statisticalResults.stream()
                    .collect(Collectors.toMap(MetricJudgement::metricName, Function.identity()));

            List<MetricJudgement> finalJudgements = llmReport.metricJudgements().stream()
                    .map(llmJudgement -> {
                        MetricJudgement stats = statsMap.get(llmJudgement.metricName());
                        return new MetricJudgement(
                                stats.metricName(),
                                stats.statisticalResult(),
                                stats.pValue(),
                                llmJudgement.interpretation()
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
