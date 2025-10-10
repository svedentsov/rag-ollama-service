package com.example.ragollama.optimization;

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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
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
     * @return {@link Mono}, который по завершении будет содержать
     * полностью собранный объект {@link QaDashboard}.
     */
    public Mono<QaDashboard> generateDashboard(String baseRef, String headRef) {
        // TODO: В реальной системе JaCoCo отчет должен извлекаться из артефактов CI,
        // а не передаваться в запросе. Для демонстрации используем заглушку.
        String mockJacocoReport = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><!DOCTYPE report PUBLIC \"-//JACOCO//DTD Report 1.1//EN\" \"report.dtd\"><report name=\"rag-ollama-service\"></report>";

        AgentContext context = new AgentContext(Map.of(
                "oldRef", baseRef,
                "newRef", headRef,
                "jacocoReportContent", mockJacocoReport,
                "days", 30
        ));

        // Запускаем все аналитические конвейеры параллельно, используя Mono
        Mono<List<AgentResult>> readinessMono = orchestratorService.invoke("release-readiness-pipeline", context);
        Mono<List<AgentResult>> testDebtMono = orchestratorService.invoke("test-debt-report-pipeline", context);
        Mono<List<AgentResult>> riskMatrixMono = orchestratorService.invoke("risk-matrix-generation-pipeline", context);

        // Когда все они завершатся, агрегируем результаты с помощью Mono.zip
        return Mono.zip(readinessMono, testDebtMono, riskMatrixMono)
                .map(tuple -> {
                    // Извлекаем типизированные отчеты из результатов агентов
                    ReleaseReadinessReport readinessReport = extractReport(tuple.getT1(), "releaseReadinessReport", ReleaseReadinessReport.class);
                    TestDebtReport testDebtReport = extractReport(tuple.getT2(), "testDebtReport", TestDebtReport.class);
                    RiskMatrixReport riskMatrixReport = extractReport(tuple.getT3(), "riskMatrixReport", RiskMatrixReport.class);

                    // Собираем дашборд
                    return buildDashboard(readinessReport, testDebtReport, riskMatrixReport);
                });
    }

    /**
     * Собирает финальный DTO дашборда из отчетов.
     *
     * @param readiness Отчет о готовности релиза.
     * @param debt      Отчет о тестовом долге.
     * @param matrix    Отчет с матрицей рисков.
     * @return Собранный объект дашборда.
     */
    private QaDashboard buildDashboard(ReleaseReadinessReport readiness, TestDebtReport debt, RiskMatrixReport matrix) {
        // Создаем builder с проверками на null
        QaDashboard.TestDebtSummary.TestDebtSummaryBuilder debtSummaryBuilder = QaDashboard.TestDebtSummary.builder();
        if (debt != null && debt.items() != null) {
            debtSummaryBuilder
                    .flakyTestCount((int) debt.items().stream().filter(i -> i.type() == DebtType.FLAKY_TEST).count())
                    .slowTestCount((int) debt.items().stream().filter(i -> i.type() == DebtType.SLOW_TEST).count())
                    .disabledTestCount((int) debt.items().stream().filter(i -> i.type() == DebtType.DISABLED_TEST).count())
                    .missingCoverageFileCount((int) debt.items().stream().filter(i -> i.type() == DebtType.MISSING_COVERAGE).count());
        }

        List<RiskMatrixItem> topRisks = (matrix != null && matrix.items() != null)
                ? matrix.items().stream().limit(5).collect(Collectors.toList())
                : List.of();

        return QaDashboard.builder()
                .releaseReadiness(readiness)
                .testDebtSummary(debtSummaryBuilder.build())
                .topRisks(topRisks)
                .build();
    }

    /**
     * Вспомогательный метод для безопасного извлечения и приведения типа отчета из AgentResult.
     *
     * @param results   Список результатов от агентов.
     * @param detailKey Ключ, по которому хранится отчет в деталях.
     * @param clazz     Класс, к которому нужно привести отчет.
     * @param <T>       Тип отчета.
     * @return Объект отчета или null, если извлечь не удалось.
     */
    private <T> T extractReport(List<AgentResult> results, String detailKey, Class<T> clazz) {
        try {
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
