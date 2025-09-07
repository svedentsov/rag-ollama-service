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
 * Финальный агент-агрегатор в конвейере безопасности ("AI CISO").
 * <p>
 * Собирает результаты от всех сканеров (SAST, RBAC, PII),
 * дедуплицирует их, приоритизирует и формирует единый отчет,
 * используя LLM для интеллектуального синтеза.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityReportAggregatorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "security-report-aggregator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Агрегирует отчеты от всех сканеров безопасности в единый, приоритизированный отчет.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        // Запускается, если есть хотя бы один из отчетов для агрегации
        return context.payload().containsKey("sastFindings") ||
                context.payload().containsKey("risks") ||
                context.payload().containsKey("privacyReport");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        Map<String, Object> allFindings = new HashMap<>();
        allFindings.put("sastFindings", context.payload().get("sastFindings"));
        allFindings.put("authRisks", context.payload().get("risks"));
        allFindings.put("privacyViolations", context.payload().get("privacyReport"));

        try {
            String findingsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allFindings);
            String promptString = promptService.render("securityReportAggregatorPrompt", Map.of("all_findings_json", findingsJson));

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

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link UnifiedSecurityReport}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link UnifiedSecurityReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private UnifiedSecurityReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, UnifiedSecurityReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Aggregator LLM вернул невалидный JSON.", e);
        }
    }
}
