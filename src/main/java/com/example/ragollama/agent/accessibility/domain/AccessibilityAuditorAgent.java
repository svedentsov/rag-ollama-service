package com.example.ragollama.agent.accessibility.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import com.example.ragollama.agent.accessibility.tools.AccessibilityScannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который проводит аудит доступности (a11y) веб-страницы.
 * <p>
 * Эталонная реализация агента-оркестратора, следующего принципам Clean Architecture.
 * Он определяет высокоуровневый бизнес-процесс, делегируя конкретные шаги
 * специализированным компонентам:
 * <ol>
 *     <li>Детерминированный поиск нарушений с помощью {@link AccessibilityScannerService}.</li>
 *     <li>Интеллектуальный анализ и обогащение найденных нарушений с помощью {@link LlmAccessibilityAnalyzer}.</li>
 * </ol>
 *
 * <p>Такая декомпозиция значительно повышает тестируемость и читаемость кода,
 * изолируя бизнес-логику от деталей реализации взаимодействия с LLM.
 * Все I/O-bound и CPU-bound операции выполняются асинхронно в управляемом пуле потоков.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessibilityAuditorAgent implements QaAgent {

    /**
     * Публичная константа, определяющая ключ для доступа к отчету о доступности
     * в деталях {@link AgentResult}. Делает контракт между агентом и потребителями
     * его результата (например, {@link com.example.ragollama.agent.accessibility.mappers.AccessibilityMapper})
     * явным и типобезопасным.
     */
    public static final String ACCESSIBILITY_REPORT_KEY = "accessibilityReport";

    private final AccessibilityScannerService scannerService;
    private final LlmAccessibilityAnalyzer llmAnalyzer;
    private final AsyncTaskExecutor applicationTaskExecutor;

    @Override
    public String getName() {
        return "accessibility-auditor";
    }

    @Override
    public String getDescription() {
        return "Анализирует HTML-код на предмет нарушений доступности (a11y) и генерирует отчет.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().get("htmlContent") instanceof String;
    }

    /**
     * Асинхронно выполняет полный конвейер аудита доступности.
     *
     * @param context Контекст, содержащий HTML-код страницы в поле `htmlContent`.
     * @return {@link CompletableFuture} с финальным отчетом {@link AgentResult}.
     * @throws ClassCastException если `htmlContent` в контексте не является строкой.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String htmlContent = (String) context.payload().get("htmlContent");
        log.info("Запуск аудита доступности для HTML-контента...");
        return CompletableFuture.supplyAsync(() -> scannerService.scan(htmlContent), applicationTaskExecutor)
                .thenCompose(violations -> {
                    if (violations.isEmpty()) {
                        log.info("Нарушений доступности не найдено. Вызов LLM не требуется.");
                        return CompletableFuture.completedFuture(createEmptySuccessResult());
                    }
                    log.info("Найдено {} нарушений доступности. Запуск LLM-анализатора.", violations.size());
                    return llmAnalyzer.analyze(violations).thenApply(this::createSuccessResultWithReport);
                });
    }

    private AgentResult createSuccessResultWithReport(AccessibilityReport report) {
        return new AgentResult(
                getName(),
                AgentResult.Status.SUCCESS,
                report.summary(),
                Map.of(ACCESSIBILITY_REPORT_KEY, report)
        );
    }

    private AgentResult createEmptySuccessResult() {
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
