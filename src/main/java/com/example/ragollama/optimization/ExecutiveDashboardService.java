package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.executive.model.*;
import com.example.ragollama.agent.strategy.model.PortfolioStrategyReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Сервис-агрегатор, который строит дашборд "Executive Command Center".
 * <p>
 * Эта версия полностью переведена на Project Reactor и использует {@link Mono#zip}
 * для эффективного параллельного выполнения всех аналитических конвейеров.
 */
@Slf4j
@Service
public class ExecutiveDashboardService {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Конструктор для внедрения зависимостей с использованием {@code @Lazy}
     * для разрыва циклической зависимости при старте приложения.
     *
     * @param orchestratorService прокси-объект для сервиса-оркестратора.
     */
    public ExecutiveDashboardService(@Lazy AgentOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    /**
     * Асинхронно генерирует полный дашборд для руководства.
     *
     * @param context Контекст с параметрами для всех анализов.
     * @return {@link Mono} с дашбордом.
     */
    public Mono<ExecutiveDashboard> generateDashboard(AgentContext context) {
        log.info("Запуск генерации полного Executive Dashboard...");

        Mono<List<AgentResult>> healthMono = orchestratorService.invoke("executive-governor-pipeline", context);
        Mono<List<AgentResult>> velocityMono = orchestratorService.invoke("engineering-velocity-pipeline", context);
        Mono<List<AgentResult>> productMono = orchestratorService.invoke("product-strategy-pipeline", context);
        Mono<List<AgentResult>> financialMono = orchestratorService.invoke("financial-roi-analysis-pipeline", context);
        Mono<List<AgentResult>> architectureMono = orchestratorService.invoke("architectural-evolution-pipeline", context);

        return Mono.zip(healthMono, velocityMono, productMono, financialMono, architectureMono)
                .map(tuple -> {
                    log.info("Все губернаторские конвейеры завершили работу. Сборка дашборда...");

                    return ExecutiveDashboard.builder()
                            .portfolioHealth(extractReport(tuple.getT1(), "portfolioStrategyReport", PortfolioStrategyReport.class))
                            .engineeringVelocity(extractReport(tuple.getT2(), "engineeringEfficiencyReport", EngineeringEfficiencyReport.class))
                            .productStrategy(extractReport(tuple.getT3(), "productStrategyReport", ProductStrategyReport.class))
                            .financialRoiReport(extractReport(tuple.getT4(), "financialRoiReport", FinancialRoiReport.class))
                            .architecturalHealth(extractReport(tuple.getT5(), "architecturalHealthReport", ArchitecturalHealthReport.class))
                            .build();
                });
    }

    private <T> T extractReport(List<AgentResult> results, String detailKey, Class<T> clazz) {
        try {
            if (results.isEmpty() || results.getLast().details() == null) {
                log.warn("Конвейер не вернул результатов или деталей для ключа '{}'", detailKey);
                return null;
            }
            Object reportData = results.getLast().details().get(detailKey);
            return clazz.isInstance(reportData) ? clazz.cast(reportData) : null;
        } catch (Exception e) {
            log.error("Не удалось извлечь отчет '{}' из результатов агента", detailKey, e);
            return null;
        }
    }
}
