package com.example.ragollama.agent.security.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.security.model.UnifiedSecurityReport;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Финальный агент-агрегатор в конвейере безопасности.
 * <p>
 * Собирает результаты от всех сканеров (SAST, DAST, Logs),
 * дедуплицирует их, приоритизирует и формирует единый отчет.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityReportAggregatorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "security-report-aggregator";
    }

    @Override
    public String getDescription() {
        return "Агрегирует отчеты от всех сканеров безопасности в единый, приоритизированный отчет.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("sastFindings") ||
                context.payload().containsKey("risks") ||
                context.payload().containsKey("privacyReport");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        Map<String, Object> allFindings = new HashMap<>();
        allFindings.put("sastFindings", context.payload().get("sastFindings"));
        allFindings.put("authRisks", context.payload().get("risks"));
        allFindings.put("privacyViolations", context.payload().get("privacyReport"));

        try {
            String findingsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allFindings);
            String promptString = promptService.render("securityReportAggregator", Map.of("all_findings_json", findingsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("unifiedSecurityReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации финального отчета безопасности", e));
        }
    }

    private UnifiedSecurityReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, UnifiedSecurityReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Aggregator LLM вернул невалидный JSON.", e);
        }
    }
}
