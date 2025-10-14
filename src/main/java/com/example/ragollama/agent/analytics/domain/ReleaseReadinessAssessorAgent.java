package com.example.ragollama.agent.analytics.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.analytics.model.ReleaseReadinessReport;
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

import java.util.Map;

/**
 * AI-агент, который выполняет комплексную оценку готовности релиза.
 * <p>
 * Является финальным шагом в конвейере, синтезируя отчеты от других
 * аналитических агентов для вынесения итогового вердикта.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseReadinessAssessorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "release-readiness-assessor";
    }

    @Override
    public String getDescription() {
        return "Анализирует совокупность метрик качества и выносит вердикт о готовности релиза.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("coverageRisks") &&
                context.payload().containsKey("qualityImpactReport") &&
                context.payload().containsKey("flakinessReport");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        log.info("ReleaseReadinessAssessorAgent: запуск финальной оценки релиза.");
        try {
            Map<String, Object> reports = Map.of(
                    "coverageReport", context.payload().get("coverageRisks"),
                    "qualityReport", context.payload().get("qualityImpactReport"),
                    "stabilityReport", context.payload().get("flakinessReport")
            );
            String reportsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reports);
            String promptString = promptService.render("releaseReadinessPrompt", Map.of("reportsJson", reportsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                    .map(tuple -> parseLlmResponse(tuple.getT1()))
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("releaseReadinessReport", report)
                    ));

        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации отчетов для LLM", e));
        }
    }

    private ReleaseReadinessReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ReleaseReadinessReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON для отчета о готовности релиза.", e);
        }
    }
}
