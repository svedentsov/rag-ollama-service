package com.example.ragollama.agent.dashboard.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.analytics.model.ReleaseReadinessReport;
import com.example.ragollama.agent.analytics.model.RiskMatrixItem;
import com.example.ragollama.agent.analytics.model.RiskMatrixReport;
import com.example.ragollama.agent.dashboard.model.QaDashboard;
import com.example.ragollama.agent.testanalysis.model.DebtType;
import com.example.ragollama.agent.testanalysis.model.TestDebtReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Сервис-агрегатор, выступающий в роли "Центра Управления QA".
 * <p>
 * Оркестрирует асинхронный запуск всех ключевых аналитических конвейеров,
 * собирает их результаты и формирует единый, холистический дашборд
 * о состоянии качества проекта.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaCommandCenterService {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Асинхронно генерирует полный дашборд состояния QA.
     *
     * @param baseRef Исходная Git-ссылка для анализа.
     * @param headRef Конечная Git-ссылка для анализа.
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * полностью собранный объект {@link QaDashboard}.
     */
    public CompletableFuture<QaDashboard> generateDashboard(String baseRef, String headRef) {
        // TODO: В реальной системе JaCoCo отчет должен извлекаться из артефактов CI,
        // а не передаваться в запросе. Для демонстрации используем заглушку.
        String mockJacocoReport = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><!DOCTYPE report PUBLIC \"-//JACOCO//DTD Report 1.1//EN\" \"report.dtd\"><report name=\"rag-ollama-service\"></report>";

        AgentContext context = new AgentContext(Map.of(
                "oldRef", baseRef,
                "newRef", headRef,
                "jacocoReportContent", mockJacocoReport,
                "days", 30
        ));

        // Запускаем все аналитические конвейеры параллельно
        CompletableFuture<List<AgentResult>> readinessFuture = orchestratorService.invokePipeline("release-readiness-pipeline", context);
        CompletableFuture<List<AgentResult>> testDebtFuture = orchestratorService.invokePipeline("test-debt-report-pipeline", context);
        CompletableFuture<List<AgentResult>> riskMatrixFuture = orchestratorService.invokePipeline("risk-matrix-generation-pipeline", context);

        // Когда все они завершатся, агрегируем результаты
        return CompletableFuture.allOf(readinessFuture, testDebtFuture, riskMatrixFuture)
                .thenApply(v -> {
                    // Извлекаем типизированные отчеты из результатов агентов
                    ReleaseReadinessReport readinessReport = extractReport(readinessFuture, "releaseReadinessReport", ReleaseReadinessReport.class);
                    TestDebtReport testDebtReport = extractReport(testDebtFuture, "testDebtReport", TestDebtReport.class);
                    RiskMatrixReport riskMatrixReport = extractReport(riskMatrixFuture, "riskMatrixReport", RiskMatrixReport.class);

                    // Собираем дашборд
                    return buildDashboard(readinessReport, testDebtReport, riskMatrixReport);
                });
    }

    /**
     * Собирает финальный DTO дашборда из отчетов.
     */
    private QaDashboard buildDashboard(ReleaseReadinessReport readiness, TestDebtReport debt, RiskMatrixReport matrix) {
        QaDashboard.TestDebtSummary debtSummary = QaDashboard.TestDebtSummary.builder()
                .flakyTestCount((int) debt.items().stream().filter(i -> i.type() == DebtType.FLAKY_TEST).count())
                .slowTestCount((int) debt.items().stream().filter(i -> i.type() == DebtType.SLOW_TEST).count())
                .disabledTestCount((int) debt.items().stream().filter(i -> i.type() == DebtType.DISABLED_TEST).count())
                .missingCoverageFileCount((int) debt.items().stream().filter(i -> i.type() == DebtType.MISSING_COVERAGE).count())
                .build();

        List<RiskMatrixItem> topRisks = matrix.items().stream()
                .limit(5)
                .collect(Collectors.toList());

        return QaDashboard.builder()
                .releaseReadiness(readiness)
                .testDebtSummary(debtSummary)
                .topRisks(topRisks)
                .build();
    }

    /**
     * Вспомогательный метод для безопасного извлечения и кастинга отчета из AgentResult.
     */
    private <T> T extractReport(CompletableFuture<List<AgentResult>> future, String detailKey, Class<T> clazz) {
        try {
            List<AgentResult> results = future.join();
            if (results.isEmpty() || results.getLast().details() == null) {
                return null;
            }
            return clazz.cast(results.getLast().details().get(detailKey));
        } catch (Exception e) {
            log.error("Не удалось извлечь отчет '{}' из результатов агента", detailKey, e);
            return null;
        }
    }
}
