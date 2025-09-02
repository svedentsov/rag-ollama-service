package com.example.ragollama.agent.accessibility.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import com.example.ragollama.agent.accessibility.model.AccessibilityViolation;
import com.example.ragollama.agent.accessibility.tools.AccessibilityScannerService;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * QA-агент, который проводит аудит доступности (a11y) веб-страницы.
 * <p>
 * Реализует гибридный подход:
 * 1. Использует детерминированный {@link AccessibilityScannerService} для поиска нарушений.
 * 2. Использует LLM для анализа, приоритизации и объяснения этих нарушений.
 * <p>
 * Эта версия полностью асинхронна, включая этап сканирования, для обеспечения
 * максимальной производительности.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessibilityAuditorAgent implements ToolAgent {

    private final AccessibilityScannerService scannerService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final Executor applicationTaskExecutor;

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
        return context.payload().get("htmlContent") instanceof String;
    }

    /**
     * Асинхронно выполняет полный конвейер аудита доступности.
     * <p>
     * Конвейер состоит из следующих шагов:
     * 1. Асинхронное сканирование HTML на предмет нарушений.
     * 2. Если нарушения найдены, они сериализуются в JSON.
     * 3. Вызывается LLM для анализа JSON и генерации резюме.
     * 4. Ответ LLM парсится и объединяется с исходными данными для формирования финального отчета.
     *
     * @param context Контекст, содержащий HTML-код страницы в поле `htmlContent`.
     * @return {@link CompletableFuture} с финальным отчетом {@link AgentResult}.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String htmlContent = (String) context.payload().get("htmlContent");

        // Шаг 1: Асинхронный детерминированный поиск нарушений.
        return CompletableFuture.supplyAsync(() -> scannerService.scan(htmlContent), applicationTaskExecutor)
                .thenCompose(violations -> {
                    if (violations.isEmpty()) {
                        return CompletableFuture.completedFuture(createSuccessResultWithoutViolations());
                    }
                    // Шаг 2: Если нарушения есть, запускаем LLM для анализа.
                    return processViolationsWithLlm(violations);
                });
    }

    /**
     * Обрабатывает найденные нарушения с помощью LLM для их анализа и обогащения.
     *
     * @param violations Список нарушений, найденных сканером.
     * @return {@link CompletableFuture} с финальным {@link AgentResult}.
     */
    private CompletableFuture<AgentResult> processViolationsWithLlm(List<AccessibilityViolation> violations) {
        try {
            String violationsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(violations);
            String promptString = promptService.render("accessibilityAudit", Map.of("violationsJson", violationsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(llmResponse -> parseLlmResponse(llmResponse, violations)) // <-- ИСПРАВЛЕНИЕ: Убран вызов .content()
                    .thenApply(this::createSuccessResultWithReport);
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации нарушений a11y", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM и объединяет его с исходными данными.
     * Этот метод инкапсулирует логику обработки потенциально невалидного вывода LLM.
     *
     * @param jsonResponse  Сырой JSON-ответ от языковой модели.
     * @param rawViolations Исходный список нарушений от сканера, который будет добавлен в финальный отчет.
     * @return Полностью собранный {@link AccessibilityReport}.
     * @throws ProcessingException если LLM вернула невалидный JSON.
     */
    private AccessibilityReport parseLlmResponse(String jsonResponse, List<AccessibilityViolation> rawViolations) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            AccessibilityReport summaryReport = objectMapper.readValue(cleanedJson, AccessibilityReport.class);
            return new AccessibilityReport(summaryReport.summary(), summaryReport.topRecommendations(), rawViolations);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от a11y LLM: {}", jsonResponse, e);
            throw new ProcessingException("LLM-аудитор вернул невалидный JSON.", e);
        }
    }

    /**
     * Создает финальный объект {@link AgentResult} на основе сгенерированного отчета.
     *
     * @param report Финальный отчет об аудите.
     * @return Результат работы агента.
     */
    private AgentResult createSuccessResultWithReport(AccessibilityReport report) {
        return new AgentResult(
                getName(),
                AgentResult.Status.SUCCESS,
                report.summary(),
                Map.of("accessibilityReport", report)
        );
    }

    /**
     * Создает объект {@link AgentResult} для случая, когда нарушения не найдены.
     *
     * @return Результат работы агента.
     */
    private AgentResult createSuccessResultWithoutViolations() {
        String summary = "Аудит завершен. Нарушений доступности не найдено.";
        AccessibilityReport emptyReport = new AccessibilityReport(summary, Collections.emptyList(), Collections.emptyList());
        return new AgentResult(
                getName(),
                AgentResult.Status.SUCCESS,
                summary,
                Map.of("accessibilityReport", emptyReport)
        );
    }
}
