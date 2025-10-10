package com.example.ragollama.agent.accessibility.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import com.example.ragollama.agent.accessibility.tools.ExternalAccessibilityScannerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

/**
 * AI-агент, который проводит аудит доступности (a11y) веб-страницы.
 * <p>
 * Эталонная реализация агента-оркестратора, следующего принципам Clean Architecture
 * и полностью адаптированного под реактивный стек. Он определяет высокоуровневый
 * бизнес-процесс, делегируя конкретные шаги специализированным компонентам:
 * <ol>
 *     <li>Детерминированный поиск нарушений через асинхронный вызов внешнего
 *     сервиса-сканера ({@link ExternalAccessibilityScannerClient}).</li>
 *     <li>Интеллектуальный анализ и обогащение найденных нарушений с помощью
 *     {@link LlmAccessibilityAnalyzer}.</li>
 * </ol>
 * <p>
 * Такая декомпозиция значительно повышает тестируемость, масштабируемость и
 * отказоустойчивость, изолируя основное приложение от тяжеловесных операций.
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

    private final ExternalAccessibilityScannerClient scannerClient;
    private final LlmAccessibilityAnalyzer llmAnalyzer;

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
     * Асинхронно выполняет полный конвейер аудита доступности в нативном реактивном стиле.
     * <p>
     * Эта реализация демонстрирует эталонный подход к работе с внешними системами:
     * вызов сканера инкапсулирован в асинхронном клиенте, который не блокирует
     * основной event loop.
     *
     * @param context Контекст, содержащий HTML-код страницы в поле `htmlContent`.
     * @return {@link Mono} с финальным отчетом {@link AgentResult}.
     * @throws ClassCastException если `htmlContent` в контексте не является строкой.
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String htmlContent = (String) context.payload().get("htmlContent");
        log.info("Запуск аудита доступности для HTML-контента...");

        return scannerClient.scan(htmlContent)
                .flatMap(violations -> {
                    if (violations.isEmpty()) {
                        log.info("Нарушений доступности не найдено. Вызов LLM не требуется.");
                        return Mono.just(createEmptySuccessResult());
                    }
                    log.info("Найдено {} нарушений доступности. Запуск LLM-анализатора.", violations.size());
                    return llmAnalyzer.analyze(violations)
                            .map(this::createSuccessResultWithReport);
                });
    }

    /**
     * Создает успешный результат с полным отчетом.
     *
     * @param report Отчет о доступности.
     * @return Стандартизированный результат работы агента.
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
     * Создает успешный результат в случае отсутствия нарушений.
     *
     * @return Стандартизированный результат работы агента.
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
