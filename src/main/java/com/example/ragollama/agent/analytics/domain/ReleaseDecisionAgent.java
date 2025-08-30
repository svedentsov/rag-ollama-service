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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, выступающий в роли "AI Release Governor".
 * <p>
 * Принимает на вход сводный отчет о качестве релиза и, на основе политики,
 * выносит финальное Go/No-Go решение.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseDecisionAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "release-decision-agent";
    }

    @Override
    public String getDescription() {
        return "Анализирует сводку по качеству релиза и выносит Go/No-Go вердикт.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("prReviewReport") && context.payload().containsKey("testDebtReport");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        try {
            String reportsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("releaseDecision", Map.of("reports_json", reportsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("releaseReadinessReport", report)
                    ));

        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчетов для Release Governor", e));
        }
    }

    private ReleaseReadinessReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ReleaseReadinessReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Release Decision LLM вернул невалидный JSON.", e);
        }
    }
}
