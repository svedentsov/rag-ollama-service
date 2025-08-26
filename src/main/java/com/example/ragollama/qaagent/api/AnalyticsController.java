package com.example.ragollama.qaagent.api;

import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.*;
import com.example.ragollama.qaagent.impl.DefectTrendMinerAgent;
import com.example.ragollama.qaagent.impl.TestMetricsAnalyzerAgent;
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
 * Предоставляет API для получения высокоуровневых инсайтов и отчетов
 * на основе исторических данных, собранных системой. Эти отчеты помогают
 * командам QA и разработки принимать data-driven решения.
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Analytics Agents API", description = "API для получения аналитических отчетов от AI-агентов")
public class AnalyticsController {

    private final TestMetricsAnalyzerAgent testMetricsAnalyzerAgent;
    private final DefectTrendMinerAgent defectTrendMinerAgent;
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
    public CompletableFuture<AgentResult> analyzeTestTrends(@Valid TestMetricsAnalysisRequest request) {
        return testMetricsAnalyzerAgent.execute(request.toAgentContext());
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
    public CompletableFuture<AgentResult> analyzeDefectTrends(@Valid DefectTrendAnalysisRequest request) {
        return defectTrendMinerAgent.execute(request.toAgentContext());
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
}
