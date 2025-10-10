package com.example.ragollama.agent.analytics.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.analytics.api.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Контроллер для запуска аналитических AI-агентов.
 * <p>
 * Эта версия полностью адаптирована для реактивного стека Spring WebFlux.
 * Все методы возвращают {@link Mono}, что обеспечивает полностью неблокирующую обработку.
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Analytics Agents API", description = "API для получения аналитических отчетов от AI-агентов")
public class AnalyticsController {

    private final AgentOrchestratorService orchestratorService;
    private final AgentResultMapper agentResultMapper;

    /**
     * Запускает анализ исторических метрик тестов и генерирует отчет о трендах.
     *
     * @param request DTO с параметрами анализа.
     * @return {@link Mono} с аналитическим отчетом.
     */
    @GetMapping("/test-trends")
    @Operation(summary = "Проанализировать тренды в метриках тестов",
            description = "Анализирует сохраненные результаты за период, вычисляет KPI и генерирует выводы и рекомендации с помощью LLM.")
    public Mono<List<AgentExecutionResponse>> analyzeTestTrends(@Valid TestMetricsAnalysisRequest request) {
        return orchestratorService.invoke("test-metrics-analysis-pipeline", request.toAgentContext())
                .map(agentResultMapper::toDto);
    }

    @GetMapping("/defect-trends")
    @Operation(summary = "Проанализировать и сгруппировать тренды в дефектах")
    public Mono<List<AgentExecutionResponse>> analyzeDefectTrends(@Valid DefectTrendAnalysisRequest request) {
        return orchestratorService.invoke("defect-trend-analysis-pipeline", request.toAgentContext())
                .map(agentResultMapper::toDto);
    }

    @PostMapping("/predict-regression")
    @Operation(summary = "Спрогнозировать риск регрессии для изменений в коде")
    public Mono<List<AgentExecutionResponse>> predictRegressionRisk(@Valid @RequestBody RegressionPredictionRequest request) {
        return orchestratorService.invoke("regression-prediction-pipeline", request.toAgentContext())
                .map(agentResultMapper::toDto);
    }

    @PostMapping("/customer-impact")
    @Operation(summary = "Проанализировать влияние изменений на пользователей (Customer Impact)")
    public Mono<List<AgentExecutionResponse>> analyzeCustomerImpact(@Valid @RequestBody CustomerImpactAnalysisRequest request) {
        return orchestratorService.invoke("customer-impact-analysis-pipeline", request.toAgentContext())
                .map(agentResultMapper::toDto);
    }

    @PostMapping("/release-readiness")
    @Operation(summary = "Получить комплексную оценку готовности релиза")
    public Mono<List<AgentExecutionResponse>> assessReleaseReadiness(@Valid @RequestBody ReleaseReadinessRequest request) {
        return orchestratorService.invoke("release-readiness-pipeline", request.toAgentContext())
                .map(agentResultMapper::toDto);
    }

    @PostMapping("/canary")
    @Operation(summary = "Провести канареечный анализ (Kayenta-like)")
    public Mono<List<AgentExecutionResponse>> analyzeCanaryDeployment(@Valid @RequestBody CanaryAnalysisRequest request) {
        return orchestratorService.invoke("canary-analysis-pipeline", request.toAgentContext())
                .map(agentResultMapper::toDto);
    }

    @PostMapping("/bug-patterns")
    @Operation(summary = "Проанализировать историю багов и выявить системные паттерны")
    public Mono<List<AgentExecutionResponse>> detectBugPatterns(@Valid @RequestBody BugPatternRequest request) {
        return orchestratorService.invoke("bug-pattern-detection-pipeline", request.toAgentContext())
                .map(agentResultMapper::toDto);
    }

    @PostMapping("/test-debt-report")
    @Operation(summary = "Получить отчет о тестовом техническом долге")
    public Mono<List<AgentExecutionResponse>> getTestDebtReport(@Valid @RequestBody TestDebtReportRequest request) {
        return orchestratorService.invoke("test-debt-report-pipeline", request.toAgentContext())
                .map(agentResultMapper::toDto);
    }

    @PostMapping("/canary/orchestrate-decision")
    @Operation(summary = "Провести анализ и оркестровать решение по canary-развертыванию")
    public Mono<List<AgentExecutionResponse>> orchestrateCanaryDecision(@Valid @RequestBody CanaryDecisionRequest request) {
        return orchestratorService.invoke("canary-decision-orchestration-pipeline", request.toAgentContext())
                .map(agentResultMapper::toDto);
    }
}
