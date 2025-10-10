package com.example.ragollama.agent.analytics.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.analytics.model.DefectEconomicReport;
import com.example.ragollama.agent.analytics.model.EconomicImpactAssessment;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.model.codeanalysis.CodeMetrics;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.tool.codeanalysis.StaticCodeAnalyzerService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * AI-агент, который моделирует экономические последствия технического долга и дефектов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefectEconomicsModelerAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final StaticCodeAnalyzerService staticAnalyzer;
    private final HistoricalDefectService historicalDefectService;
    private final CostEstimationService costService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "defect-economics-modeler";
    }

    @Override
    public String getDescription() {
        return "Оценивает стоимость исправления и стоимость бездействия для дефекта или рискованного кода.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("filePath") && context.payload().containsKey("ref");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String filePath = (String) context.payload().get("filePath");
        String ref = (String) context.payload().get("ref");

        Mono<String> contentMono = gitApiClient.getFileContent(filePath, ref);
        Mono<CodeMetrics> metricsMono = contentMono.map(staticAnalyzer::analyze);
        Mono<Long> failuresMono = Mono.fromCallable(() ->
                        historicalDefectService.getFailureCountsByClass(90).getOrDefault(filePath, 0L))
                .subscribeOn(Schedulers.boundedElastic());

        return Mono.zip(contentMono, metricsMono, failuresMono)
                .flatMap(tuple -> {
                    Map<String, Object> dossier = Map.of(
                            "filePath", filePath,
                            "codeMetrics", tuple.getT2(),
                            "historicalFailureCount", tuple.getT3(),
                            "codeContent", tuple.getT1()
                    );
                    try {
                        String dossierJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dossier);
                        String promptString = promptService.render("defectEconomicsPrompt", Map.of("dossierJson", dossierJson));
                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessingException("Ошибка сериализации досье дефекта", e));
                    }
                })
                .map(this::parseLlmResponse)
                .map(assessment -> {
                    double remediationCost = costService.calculateRemediationCost(assessment.estimatedDevHoursToFix());
                    double inactionCost = costService.calculateInactionCost(assessment.estimatedSupportTicketsPerMonth());
                    DefectEconomicReport report = new DefectEconomicReport(filePath, remediationCost, inactionCost, assessment.summary(), assessment);
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Экономическая модель для " + filePath + " успешно построена.",
                            Map.of("economicReport", report)
                    );
                });
    }

    private EconomicImpactAssessment parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, EconomicImpactAssessment.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON для экономической оценки.", e);
        }
    }
}
