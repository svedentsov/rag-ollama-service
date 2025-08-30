package com.example.ragollama.agent.dashboard.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.dashboard.model.PRReviewReport;
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
 * AI-агент, выступающий в роли "AI Team Lead".
 * <p>
 * Агрегирует отчеты от всех аналитических агентов (покрытие, безопасность, архитектура)
 * и синтезирует из них единый, исчерпывающий отчет-ревью для Pull Request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrReviewAggregatorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "pr-review-aggregator";
    }

    @Override
    public String getDescription() {
        return "Агрегирует все отчеты по качеству в единый комментарий для Pull Request.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        try {
            String analysisJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("prReviewAggregator", Map.of("analysis_reports_json", analysisJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("prReviewReport", report)
                    ));

        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчетов для PR-ревью", e));
        }
    }

    private PRReviewReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, PRReviewReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("PR Review Aggregator LLM вернул невалидный JSON.", e);
        }
    }
}
