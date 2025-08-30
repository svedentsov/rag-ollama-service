package com.example.ragollama.agent.autonomy;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-фасад для агрегации данных от всех аналитических конвейеров.
 * <p>
 * Его единственная задача — запустить все необходимые аналитические
 * пайплайны и собрать их результаты в единую структуру данных ("Отчет о Здоровье")
 * для передачи в {@link AutonomousQALeadAgent}.
 */
@Service
@RequiredArgsConstructor
public class ProjectHealthAggregatorService {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Асинхронно запускает все аналитические конвейеры и собирает их результаты.
     *
     * @param context Контекст с параметрами для анализа (например, период).
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * карту с отчетами от каждого аналитического конвейера.
     */
    public CompletableFuture<Map<String, Object>> aggregateHealthReports(AgentContext context) {
        CompletableFuture<List<AgentResult>> testDebtFuture = orchestratorService.invokePipeline("test-debt-report-pipeline", context);
        CompletableFuture<List<AgentResult>> securityAuditFuture = orchestratorService.invokePipeline("security-audit-pipeline", context);

        return CompletableFuture.allOf(testDebtFuture, securityAuditFuture)
                .thenApply(v -> {
                    Map<String, Object> healthReport = new HashMap<>();
                    healthReport.put("testDebt", testDebtFuture.join());
                    healthReport.put("securityAudit", securityAuditFuture.join());
                    return healthReport;
                });
    }
}
