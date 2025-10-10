package com.example.ragollama.agent.performance.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.analytics.domain.HistoricalDefectService;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.agent.performance.model.PerformanceImpactReport;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.tool.codeanalysis.StaticCodeAnalyzerService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI-агент, который прогнозирует влияние изменений в коде на производительность.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PerformancePredictorAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final StaticCodeAnalyzerService staticAnalyzer;
    private final HistoricalDefectService historicalDefectService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "performance-predictor-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Прогнозирует влияние изменений кода на производительность (latency, CPU, memory).";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles") && context.payload().containsKey("newRef");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
        String newRef = (String) context.payload().get("newRef");
        Map<String, Long> historicalFailures = historicalDefectService.getFailureCountsByClass(90);

        return Flux.fromIterable(changedFiles)
                .filter(file -> file.endsWith(".java") && file.startsWith("src/main/java"))
                .flatMap(file -> gitApiClient.getFileContent(file, newRef)
                        .map(content -> Map.of(
                                "filePath", file,
                                "codeMetrics", staticAnalyzer.analyze(content),
                                "historicalFailureCount", historicalFailures.getOrDefault(file, 0L),
                                "codeContent", content
                        ))
                )
                .collectList()
                .flatMap(profiles -> {
                    if (profiles.isEmpty()) {
                        return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Не найдено Java-файлов для анализа.", Map.of()));
                    }
                    try {
                        String profilesJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profiles);
                        String promptString = promptService.render("performancePredictor", Map.of("dossier_json", profilesJson));
                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .map(this::parseLlmResponse)
                                .map(report -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        "Прогноз влияния на производительность завершен.",
                                        Map.of("performanceImpactReport", report)
                                ));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessingException("Ошибка сериализации профилей производительности", e));
                    }
                });
    }

    private PerformanceImpactReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, PerformanceImpactReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Performance Predictor LLM вернул невалидный JSON.", e);
        }
    }
}
