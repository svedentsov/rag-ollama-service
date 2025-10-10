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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

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
    private final JsonExtractorUtil jsonExtractorUtil;

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
    public Mono<AgentResult> execute(AgentContext context) {
        List<Map<String, Object>> baselineData = (List<Map<String, Object>>) context.payload().get("baselineData");
        List<Map<String, Object>> productionData = (List<Map<String, Object>>) context.payload().get("productionData");

        return Mono.fromCallable(() -> analysisService.analyze(baselineData, productionData))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(statisticalResults -> {
                    if (statisticalResults.isEmpty()) {
                        return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Нет общих признаков для анализа дрейфа.", Map.of()));
                    }
                    try {
                        String statsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statisticalResults);
                        String promptString = promptService.render("mlDriftGuardPrompt", Map.of("statistical_report_json", statsJson));

                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .map(this::parseLlmResponse)
                                .map(report -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        report.executiveSummary(),
                                        Map.of("driftReport", report)
                                ));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessingException("Ошибка сериализации отчета о дрейфе", e));
                    }
                });
    }

    private DriftReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, DriftReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от ML Drift Guard LLM: {}", jsonResponse, e);
            throw new ProcessingException("ML Drift Guard LLM вернул невалидный JSON.", e);
        }
    }
}
