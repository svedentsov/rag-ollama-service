package com.example.ragollama.agent.accessibility.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
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
public class AccessibilityAuditorAgent implements ToolAgent {

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

    /**
     * {@inheritDoc}
     *
     * @return Уникальное имя агента.
     */
    @Override
    public String getName() {
        return "accessibility-auditor";
    }

    /**
     * {@inheritDoc}
     *
     * @return Человекочитаемое описание назначения агента.
     */
    @Override
    public String getDescription() {
        return "Анализирует HTML-код на предмет нарушений доступности (a11y) и генерирует отчет.";
    }

    /**
     * {@inheritDoc}
     *
     * @param context Контекст выполнения, который должен содержать `htmlContent`.
     * @return {@code true}, если контекст содержит необходимый ключ.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().get("htmlContent") instanceof String;
    }

    /**
     * Асинхронно выполняет полный конвейер аудита доступности.
     * <p>
     * Конвейер состоит из следующих шагов, выполняемых в выделенном пуле потоков:
     * <ol>
     *     <li>Асинхронное сканирование HTML на предмет нарушений.</li>
     *     <li>Если нарушения найдены, они асинхронно передаются в {@link LlmAccessibilityAnalyzer} для анализа,
     *         после чего результат преобразуется в {@link AgentResult}.</li>
     *     <li>Если нарушений нет, немедленно формируется финальный {@link AgentResult} без вызова LLM,
     *         что экономит ресурсы.</li>
     * </ol>
     *
     * @param context Контекст, содержащий HTML-код страницы в поле `htmlContent`.
     * @return {@link CompletableFuture} с финальным отчетом {@link AgentResult}.
     * @throws ClassCastException если `htmlContent` в контексте не является строкой.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String htmlContent = (String) context.payload().get("htmlContent");
        log.info("Запуск аудита доступности для HTML-контента...");
        // Шаг 1: Асинхронный детерминированный поиск нарушений в выделенном пуле.
        return CompletableFuture.supplyAsync(() -> scannerService.scan(htmlContent), applicationTaskExecutor)
                .thenComposeAsync(violations -> {
                    if (violations.isEmpty()) {
                        log.info("Нарушений доступности не найдено. Вызов LLM не требуется.");
                        return CompletableFuture.completedFuture(createEmptySuccessResult());
                    }
                    // Шаг 2: Если нарушения есть, асинхронно запускаем LLM для анализа.
                    log.info("Найдено {} нарушений доступности. Запуск LLM-анализатора.", violations.size());
                    return llmAnalyzer.analyze(violations).thenApply(this::createSuccessResultWithReport);
                }, applicationTaskExecutor);
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
     * Создает финальный объект {@link AgentResult} для случая, когда нарушения не найдены.
     *
     * @return Успешный результат работы агента с пустым отчетом.
     */
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
