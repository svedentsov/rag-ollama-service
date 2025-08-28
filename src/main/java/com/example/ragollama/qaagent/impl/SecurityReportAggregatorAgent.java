package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.SecurityFinding;
import com.example.ragollama.qaagent.model.UnifiedSecurityReport;
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

import java.util.ArrayList;
import java.util.List;
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
        // Запускается, если есть хотя бы один из отчетов
        return context.payload().containsKey("sastFindings") || context.payload().containsKey("logAnalysisFindings");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<SecurityFinding> sastFindings = (List<SecurityFinding>) context.payload().getOrDefault("sastFindings", List.of());
        List<SecurityFinding> logFindings = (List<SecurityFinding>) context.payload().getOrDefault("logAnalysisFindings", List.of());

        List<SecurityFinding> allFindings = new ArrayList<>();
        allFindings.addAll(sastFindings);
        allFindings.addAll(logFindings);

        if (allFindings.isEmpty()) {
            return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Полный аудит безопасности завершен. Уязвимостей не найдено.", Map.of()));
        }

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
            log.error("Не удалось распарсить JSON-ответ от Aggregator LLM: {}", jsonResponse, e);
            throw new ProcessingException("Aggregator LLM вернул невалидный JSON.", e);
        }
    }
}
