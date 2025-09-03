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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который проводит аудит доступности (a11y) веб-страницы.
 *
 * <p>Реализует гибридный подход:
 * <ol>
 *     <li>Использует детерминированный инструмент {@link AccessibilityScannerService} для поиска нарушений.</li>
 *     <li>Использует LLM для анализа, приоритизации и объяснения этих нарушений на естественном языке.</li>
 * </ol>
 * <p>
 * Этот класс является чистым оркестратором, делегируя парсинг ответа LLM
 * специализированному компоненту {@link AccessibilityReportParser}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessibilityAuditorAgent implements ToolAgent {

    /**
     * Публичная константа, определяющая ключ для доступа к отчету о доступности
     * в деталях {@link AgentResult}. Делает контракт между агентом и потребителями
     * его результата явным и типобезопасным.
     */
    public static final String ACCESSIBILITY_REPORT_KEY = "accessibilityReport";

    private final AccessibilityScannerService scannerService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final AccessibilityReportParser reportParser;
    private final AsyncTaskExecutor applicationTaskExecutor;

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
     *
     * <p>Конвейер состоит из следующих шагов:
     * <ol>
     *     <li>Асинхронное сканирование HTML на предмет нарушений в управляемом пуле потоков.</li>
     *     <li>Если нарушения найдены, они сериализуются в JSON.</li>
     *     <li>Вызывается LLM для анализа JSON и генерации резюме.</li>
     *     <li>Ответ LLM парсится и объединяется с исходными данными для формирования финального отчета.</li>
     * </ol>
     *
     * @param context Контекст, содержащий HTML-код страницы в поле `htmlContent`.
     * @return {@link CompletableFuture} с финальным отчетом {@link AgentResult}.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String htmlContent = (String) context.payload().get("htmlContent");

        // Шаг 1: Асинхронный детерминированный поиск нарушений.
        return CompletableFuture.supplyAsync(() -> scannerService.scan(htmlContent), applicationTaskExecutor)
                .thenComposeAsync(violations -> {
                    if (violations.isEmpty()) {
                        return CompletableFuture.completedFuture(createSuccessResultWithoutViolations());
                    }
                    // Шаг 2: Если нарушения есть, запускаем LLM для анализа.
                    return processViolationsWithLlm(violations);
                }, applicationTaskExecutor);
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
                    .thenApply(llmResponse -> reportParser.parse(llmResponse, violations))
                    .thenApply(this::createSuccessResultWithReport);
        } catch (JsonProcessingException e) {
            log.error("Критическая ошибка: не удалось сериализовать список нарушений a11y в JSON.", e);
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации нарушений a11y", e));
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
                Map.of(ACCESSIBILITY_REPORT_KEY, report)
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
                Map.of(ACCESSIBILITY_REPORT_KEY, emptyReport)
        );
    }
}
