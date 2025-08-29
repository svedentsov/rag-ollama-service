package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.ArchitecturalReviewReport;
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
 * Мета-агент, выступающий в роли "AI Architecture Governor".
 * <p>
 * Агрегирует отчеты от всех аналитических агентов (архитектура, качество тестов,
 * производительность, приватность) и синтезирует из них единый,
 * исчерпывающий отчет-ревью для Pull Request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchitectureReviewSynthesizerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "architecture-review-synthesizer";
    }

    @Override
    public String getDescription() {
        return "Агрегирует все отчеты по качеству в единый архитектурный вердикт.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        try {
            String analysisJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("architecturalReviewSynthesizer", Map.of("analysis_reports_json", analysisJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("architecturalReviewReport", report)
                    ));

        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчетов для архитектурного ревью", e));
        }
    }

    private ArchitecturalReviewReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ArchitecturalReviewReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Architecture Review LLM вернул невалидный JSON.", e);
        }
    }
}
