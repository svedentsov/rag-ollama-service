package com.example.ragollama.agent.analytics.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.analytics.model.CodeQualityImpactReport;
import com.example.ragollama.agent.git.tools.GitApiClient;
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
 * AI-агент, который оценивает влияние качества кода измененных файлов
 * на будущую стабильность и поддерживаемость проекта.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeQualityImpactEstimatorAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final StaticCodeAnalyzerService staticAnalyzer;
    private final HistoricalDefectService historicalDefectService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "code-quality-impact-estimator";
    }

    @Override
    public String getDescription() {
        return "Оценивает риск регрессии и техдолга, анализируя статические метрики кода и историю дефектов.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles") && context.payload().containsKey("newRef");
    }

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
                                "historicalFailureCount", historicalFailures.getOrDefault(file, 0L)
                        ))
                )
                .collectList()
                .flatMap(profiles -> {
                    if (profiles.isEmpty()) {
                        return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Не найдено измененных Java-файлов для анализа.", Map.of()));
                    }
                    try {
                        String profilesJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profiles);
                        String promptString = promptService.render("codeQualityImpactPrompt", Map.of("riskDataJson", profilesJson));
                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                                .map(tuple -> parseLlmResponse(tuple.getT1()))
                                .map(report -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        "Анализ влияния качества кода завершен.",
                                        Map.of("qualityImpactReport", report)
                                ));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessingException("Ошибка сериализации профилей риска", e));
                    }
                });
    }

    private CodeQualityImpactReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, CodeQualityImpactReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON для отчета о качестве кода.", e);
        }
    }
}
