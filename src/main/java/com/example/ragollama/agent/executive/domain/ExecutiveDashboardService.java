package com.example.ragollama.agent.executive.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.executive.model.*;
import com.example.ragollama.agent.strategy.model.PortfolioStrategyReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-агрегатор, который строит дашборд "Executive Command Center".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutiveDashboardService {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Асинхронно генерирует полный дашборд для руководства.
     *
     * @param context Контекст с параметрами для всех анализов.
     * @return {@link CompletableFuture} с дашбордом.
     */
    public CompletableFuture<ExecutiveDashboard> generateDashboard(AgentContext context) {
        log.info("Запуск генерации полного Executive Dashboard...");

        // Шаг 1: Асинхронно и параллельно запускаем все губернаторские конвейеры
        CompletableFuture<List<AgentResult>> healthFuture =
                orchestratorService.invoke("executive-governor-pipeline", context);
        CompletableFuture<List<AgentResult>> velocityFuture =
                orchestratorService.invoke("engineering-velocity-pipeline", context);
        CompletableFuture<List<AgentResult>> productFuture =
                orchestratorService.invoke("product-strategy-pipeline", context);
        CompletableFuture<List<AgentResult>> financialFuture =
                orchestratorService.invoke("financial-roi-analysis-pipeline", context);
        CompletableFuture<List<AgentResult>> architectureFuture =
                orchestratorService.invoke("architectural-evolution-pipeline", context);

        // Шаг 2: Дожидаемся завершения всех и собираем результаты в единый DTO
        return CompletableFuture.allOf(healthFuture, velocityFuture, productFuture, financialFuture, architectureFuture)
                .thenApply(v -> {
                    log.info("Все губернаторские конвейеры завершили работу. Сборка дашборда...");

                    return ExecutiveDashboard.builder()
                            .portfolioHealth(extractReport(healthFuture, "portfolioStrategyReport", PortfolioStrategyReport.class))
                            .engineeringVelocity(extractReport(velocityFuture, "engineeringEfficiencyReport", EngineeringEfficiencyReport.class))
                            .productStrategy(extractReport(productFuture, "productStrategyReport", ProductStrategyReport.class))
                            .financialRoiReport(extractReport(financialFuture, "financialRoiReport", FinancialRoiReport.class))
                            .architecturalHealth(extractReport(architectureFuture, "architecturalHealthReport", ArchitecturalHealthReport.class))
                            .build();
                });
    }

    private <T> T extractReport(CompletableFuture<List<AgentResult>> future, String detailKey, Class<T> clazz) {
        try {
            List<AgentResult> results = future.join();
            if (results.isEmpty() || results.getLast().details() == null) {
                log.warn("Конвейер не вернул результатов или деталей для ключа '{}'", detailKey);
                return null;
            }
            Object reportData = results.getLast().details().get(detailKey);
            if (clazz.isInstance(reportData)) {
                return clazz.cast(reportData);
            }
            return null;
        } catch (Exception e) {
            log.error("Не удалось извлечь отчет '{}' из результатов агента", detailKey, e);
            return null;
        }
    }
}
