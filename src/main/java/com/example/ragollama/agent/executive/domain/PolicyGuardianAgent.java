package com.example.ragollama.agent.executive.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.executive.model.PolicyComplianceReport;
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
 * Мета-агент, выступающий в роли "Губернатора Политик и Безопасности".
 * <p>
 * Агрегирует отчеты от всех агентов-валидаторов, приоритизирует их
 * и выносит финальный вердикт о соответствии изменений.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyGuardianAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "policy-guardian-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Агрегирует все отчеты о нарушениях и выносит финальный вердикт о соответствии.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        // Запускается как финальный агент, если есть хоть какие-то данные для анализа
        return context.payload().containsKey("changedFiles");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        try {
            String analysisJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("policyGuardian", Map.of("analysis_reports_json", analysisJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("policyComplianceReport", report)
                    ));

        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчетов для Policy Guardian", e));
        }
    }

    private PolicyComplianceReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, PolicyComplianceReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Policy Guardian LLM вернул невалидный JSON.", e);
        }
    }
}
