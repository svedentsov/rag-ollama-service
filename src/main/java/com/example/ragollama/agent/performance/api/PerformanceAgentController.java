package com.example.ragollama.agent.performance.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.performance.api.dto.PerformancePredictionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для AI-агентов, выполняющих анализ производительности.
 */
@RestController
@RequestMapping("/api/v1/agents/performance")
@RequiredArgsConstructor
@Tag(name = "Performance Agents", description = "API для анализа и прогнозирования производительности")
public class PerformanceAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает конвейер для прогнозирования влияния изменений на производительность.
     *
     * @param request DTO с Git-ссылками, определяющими диапазон анализа.
     * @return {@link CompletableFuture} с результатом, содержащим отчет о потенциальных рисках.
     */
    @PostMapping("/predict-impact")
    @Operation(summary = "Спрогнозировать влияние изменений на производительность")
    public CompletableFuture<List<AgentResult>> predictPerformanceImpact(@Valid @RequestBody PerformancePredictionRequest request) {
        return orchestratorService.invoke("performance-prediction-pipeline", request.toAgentContext());
    }
}
