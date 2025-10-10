package com.example.ragollama.agent.metrics.api;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.analytics.api.dto.TestMetricsAnalysisRequest;
import com.example.ragollama.agent.metrics.api.dto.TestMetricsCollectionRequest;
import com.example.ragollama.agent.metrics.domain.TestMetricsCollectorAgent;
import com.example.ragollama.agent.testanalysis.domain.TestMetricsAnalyzerAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Контроллер для сбора и анализа метрик процесса тестирования.
 * <p>
 * Предоставляет API для внешних систем (например, CI/CD) для отправки
 * результатов тестовых прогонов и для пользователей (QA-инженеров, менеджеров)
 * для запроса аналитических отчетов по этим данным.
 */
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Test Metrics API", description = "API для сбора и анализа метрик QA")
public class TestMetricsController {

    private final TestMetricsCollectorAgent collectorAgent;
    private final TestMetricsAnalyzerAgent analyzerAgent;

    /**
     * Принимает результаты тестового прогона из CI/CD для последующего сохранения.
     *
     * @param request DTO с XML-отчетом и метаданными сборки.
     * @return {@link Mono} с результатом операции.
     */
    @PostMapping("/test-runs")
    @Operation(summary = "Сохранить результаты тестового прогона",
            description = "Принимает JUnit XML отчет из CI/CD и сохраняет агрегированные метрики в БД.")
    public Mono<AgentResult> collectTestMetrics(@Valid @RequestBody TestMetricsCollectionRequest request) {
        AgentContext context = new AgentContext(Map.of(
                "junitXmlContent", request.junitXmlContent(),
                "commitHash", request.commitHash(),
                "branchName", request.branchName()
        ));
        return collectorAgent.execute(context);
    }

    /**
     * Запускает анализ исторических метрик и генерирует отчет на естественном языке.
     *
     * @param request DTO с параметрами анализа (например, период в днях).
     * @return {@link Mono} с аналитическим отчетом.
     */
    @GetMapping("/analysis")
    @Operation(summary = "Проанализировать исторические метрики тестов",
            description = "Анализирует сохраненные результаты за период, вычисляет KPI и генерирует выводы с помощью LLM.")
    public Mono<AgentResult> analyzeTestMetrics(@Valid TestMetricsAnalysisRequest request) {
        AgentContext context = new AgentContext(Map.of("days", request.days()));
        return analyzerAgent.execute(context);
    }
}
