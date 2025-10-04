package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-агрегатор, собирающий все отчеты о состоянии проекта.
 * <p>
 * Этот класс больше не является агентом (`ToolAgent`), а представляет собой
 * чистый сервисный слой. Это разрывает циклическую зависимость, так как
 * `AgentOrchestratorService` больше не должен знать об этом компоненте при
 * своем создании.
 */
@Slf4j
@Service
public class ProjectHealthAggregatorService {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Конструктор для внедрения зависимостей.
     * <p>
     * Аннотация {@code @Lazy} используется для разрыва
     * циклической зависимости во время старта приложения. Spring внедрит сюда
     * прокси-объект, а реальный бин `AgentOrchestratorService` будет
     * разрешен только при первом вызове метода, когда он уже будет полностью создан.
     *
     * @param orchestratorService прокси-объект для сервиса-оркестратора.
     */
    public ProjectHealthAggregatorService(@Lazy AgentOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    /**
     * Асинхронно собирает полную картину о состоянии проекта, параллельно
     * запуская все необходимые аналитические конвейеры.
     *
     * @param context Контекст, содержащий параметры для анализа (например, `analysisPeriodDays`).
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * карту с агрегированными отчетами о здоровье проекта.
     */
    public CompletableFuture<Map<String, Object>> aggregateHealthReports(AgentContext context) {
        log.info("Запуск агрегации отчетов о здоровье проекта...");
        CompletableFuture<List<AgentResult>> testDebtFuture = orchestratorService.invoke("test-debt-report-pipeline", context);
        CompletableFuture<List<AgentResult>> bugPatternFuture = orchestratorService.invoke("bug-pattern-detection-pipeline", context);

        return CompletableFuture.allOf(testDebtFuture, bugPatternFuture)
                .thenApply(v -> {
                    Map<String, Object> healthReport = new HashMap<>();
                    testDebtFuture.join().stream()
                            .reduce((first, second) -> second) // Берем результат последнего агента в конвейере
                            .ifPresent(r -> healthReport.put("testDebtReport", r.details().get("testDebtReport")));

                    bugPatternFuture.join().stream()
                            .reduce((first, second) -> second)
                            .ifPresent(r -> healthReport.put("bugPatternReport", r.details().get("bugPatternReport")));

                    log.info("Агрегация отчетов о здоровье проекта завершена. Собрано {} отчетов.", healthReport.size());
                    return healthReport;
                });
    }
}