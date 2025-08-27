package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.AccessibilityReport;
import com.example.ragollama.qaagent.model.AccessibilityViolation;
import com.example.ragollama.qaagent.tools.AccessibilityScannerService;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который проводит аудит доступности (a11y) веб-страницы.
 * <p>
 * Реализует гибридный подход:
 * 1. Использует детерминированный {@link AccessibilityScannerService} для поиска нарушений.
 * 2. Использует LLM для анализа, приоритизации и объяснения этих нарушений.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessibilityAuditorAgent implements ToolAgent {

    private final AccessibilityScannerService scannerService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "accessibility-auditor";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует HTML-код на предмет нарушений доступности (a11y) и генерирует отчет.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("htmlContent");
    }

    /**
     * Асинхронно выполняет аудит доступности.
     *
     * @param context Контекст, содержащий HTML-код страницы.
     * @return {@link CompletableFuture} с финальным отчетом.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String htmlContent = (String) context.payload().get("htmlContent");

        // Шаг 1: Детерминированный поиск нарушений
        List<AccessibilityViolation> violations = scannerService.scan(htmlContent);

        if (violations.isEmpty()) {
            return CompletableFuture.completedFuture(new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    "Аудит завершен. Нарушений доступности не найдено.",
                    Map.of("accessibilityReport", new AccessibilityReport("Нарушений не найдено.", List.of(), List.of()))
            ));
        }

        // Шаг 2: Вызов LLM для анализа и обогащения отчета
        try {
            String violationsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(violations);
            String promptString = promptService.render("accessibilityAudit", Map.of("violationsJson", violationsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(llmResponse -> parseLlmResponse(llmResponse, violations))
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("accessibilityReport", report)
                    ));

        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации нарушений a11y", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM и объединяет его с исходными данными.
     */
    private AccessibilityReport parseLlmResponse(String jsonResponse, List<AccessibilityViolation> rawViolations) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            AccessibilityReport summaryReport = objectMapper.readValue(cleanedJson, AccessibilityReport.class);
            // Объединяем: берем summary и recommendations от LLM, а violations - из исходного сканера
            return new AccessibilityReport(summaryReport.summary(), summaryReport.topRecommendations(), rawViolations);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от a11y LLM: {}", jsonResponse, e);
            throw new ProcessingException("LLM-аудитор вернул невалидный JSON.", e);
        }
    }
}
