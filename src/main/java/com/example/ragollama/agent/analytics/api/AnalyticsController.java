package com.example.ragollama.agent.analytics.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.analytics.api.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для запуска аналитических AI-агентов.
 * <p>
 * Эта версия контроллера строго следует принципам Clean Architecture:
 * <ul>
 *     <li>Он не имеет прямых зависимостей от конкретных реализаций агентов.</li>
 *     <li>Все взаимодействия с бизнес-логикой осуществляются через единый фасад
 *         {@link AgentOrchestratorService}, который запускает именованные конвейеры.</li>
 *     <li>Это полностью отделяет Web-слой от деталей реализации Application-слоя,
 *         повышая гибкость и тестируемость.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Analytics Agents API", description = "API для получения аналитических отчетов от AI-агентов")
public class AnalyticsController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает анализ исторических метрик тестов и генерирует отчет о трендах.
     * <p>
     * Агент-аналитик извлекает данные о тестовых прогонах за указанный период,
     * агрегирует их и использует LLM для выявления трендов, аномалий и
     * формулирования действенных рекомендаций.
     *
     * @param request DTO с параметрами анализа (например, период в днях).
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * аналитический отчет от LLM в формате Markdown.
     */
    @GetMapping("/test-trends")
    @Operation(summary = "Проанализировать тренды в метриках тестов",
            description = "Анализирует сохраненные результаты за период, вычисляет KPI и генерирует выводы и рекомендации с помощью LLM.")
    public CompletableFuture<List<AgentResult>> analyzeTestTrends(@Valid TestMetricsAnalysisRequest request) {
        return orchestratorService.invokePipeline("test-metrics-analysis-pipeline", request.toAgentContext());
    }

    /**
     * Запускает анализ и кластеризацию баг-репортов за указанный период.
     * <p>
     * Агент использует семантический поиск для группировки похожих дефектов
     * в кластеры, а затем с помощью LLM определяет общую тему или первопричину
     * для каждого кластера, выявляя скрытые системные проблемы.
     *
     * @param request DTO с параметрами анализа.
     * @return {@link CompletableFuture} с отчетом, содержащим кластеры дефектов.
     */
    @GetMapping("/defect-trends")
    @Operation(summary = "Проанализировать и сгруппировать тренды в дефектах")
    public CompletableFuture<List<AgentResult>> analyzeDefectTrends(@Valid DefectTrendAnalysisRequest request) {
        return orchestratorService.invokePipeline("defect-trend-analysis-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер для прогнозирования регрессионных рисков.
     *
     * @param request DTO с Git-ссылками и отчетом о покрытии.
     * @return {@link CompletableFuture} с отчетом о рисках.
     */
    @PostMapping("/predict-regression")
    @Operation(summary = "Спрогнозировать риск регрессии для изменений в коде")
    public CompletableFuture<List<AgentResult>> predictRegressionRisk(@Valid @RequestBody RegressionPredictionRequest request) {
        return orchestratorService.invokePipeline("regression-prediction-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер для анализа влияния изменений на конечных пользователей.
     *
     * @param request DTO с Git-ссылками.
     * @return {@link CompletableFuture} с отчетом о влиянии на клиентов.
     */
    @PostMapping("/customer-impact")
    @Operation(summary = "Проанализировать влияние изменений на пользователей (Customer Impact)")
    public CompletableFuture<List<AgentResult>> analyzeCustomerImpact(@Valid @RequestBody CustomerImpactAnalysisRequest request) {
        return orchestratorService.invokePipeline("customer-impact-analysis-pipeline", request.toAgentContext());
    }

    /**
     * Запускает полный конвейер для оценки готовности релиза.
     *
     * @param request DTO со всеми необходимыми данными для анализа (Git-ссылки, отчет о покрытии).
     * @return {@link CompletableFuture} с финальным отчетом о готовности релиза.
     */
    @PostMapping("/release-readiness")
    @Operation(summary = "Получить комплексную оценку готовности релиза")
    public CompletableFuture<List<AgentResult>> assessReleaseReadiness(@Valid @RequestBody ReleaseReadinessRequest request) {
        return orchestratorService.invokePipeline("release-readiness-pipeline", request.toAgentContext());
    }

    /**
     * Выполняет канареечный анализ на основе предоставленных метрик.
     *
     * @param request DTO с данными метрик для baseline и canary.
     * @return {@link CompletableFuture} с финальным отчетом и Go/No-Go решением.
     */
    @PostMapping("/canary")
    @Operation(summary = "Провести канареечный анализ (Kayenta-like)",
            description = "Принимает наборы метрик, выполняет статистический анализ и использует AI для вынесения Go/No-Go вердикта.")
    public CompletableFuture<List<AgentResult>> analyzeCanaryDeployment(@Valid @RequestBody CanaryAnalysisRequest request) {
        return orchestratorService.invokePipeline("canary-analysis-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для анализа всей истории багов и выявления системных паттернов.
     *
     * @param request DTO с периодом анализа.
     * @return {@link CompletableFuture} с финальным отчетом о найденных паттернах.
     */
    @PostMapping("/bug-patterns")
    @Operation(summary = "Проанализировать историю багов и выявить системные паттерны")
    public CompletableFuture<List<AgentResult>> detectBugPatterns(@Valid @RequestBody BugPatternRequest request) {
        return orchestratorService.invokePipeline("bug-pattern-detection-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для анализа и составления отчета о тестовом техническом долге.
     *
     * @param request DTO для запроса (в данный момент пустой).
     * @return {@link CompletableFuture} с финальным отчетом.
     */
    @PostMapping("/test-debt-report")
    @Operation(summary = "Получить отчет о тестовом техническом долге")
    public CompletableFuture<List<AgentResult>> getTestDebtReport(@Valid @RequestBody TestDebtReportRequest request) {
        return orchestratorService.invokePipeline("test-debt-report-pipeline", request.toAgentContext());
    }

    /**
     * Запускает полный конвейер для анализа и принятия решения по канареечному развертыванию.
     *
     * @param request DTO с данными метрик и политикой принятия решений.
     * @return {@link CompletableFuture} с результатом выполненного плана действий.
     */
    @PostMapping("/canary/orchestrate-decision")
    @Operation(summary = "Провести анализ и оркестровать решение по canary-развертыванию")
    public CompletableFuture<List<AgentResult>> orchestrateCanaryDecision(@Valid @RequestBody CanaryDecisionRequest request) {
        return orchestratorService.invokePipeline("canary-decision-orchestration-pipeline", request.toAgentContext());
    }
}
