package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.api.dto.TraceAnalysisRequest;
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
 * Контроллер для AI-агентов, отвечающих за анализ данных наблюдаемости.
 */
@RestController
@RequestMapping("/api/v1/observability")
@RequiredArgsConstructor
@Tag(name = "Observability Director", description = "API для AI-управляемого анализа производительности")
public class ObservabilityController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает конвейер для сбора и анализа данных распределенной трассировки.
     *
     * @param request DTO с ID запроса для анализа.
     * @return {@link CompletableFuture} с финальным отчетом о производительности.
     */
    @PostMapping("/analyze-trace")
    @Operation(summary = "Проанализировать распределенный трейс для запроса",
            description = "Имитирует сбор данных из Jaeger/Zipkin и использует AI для поиска узких мест.")
    public CompletableFuture<List<AgentResult>> analyzeTrace(@Valid @RequestBody TraceAnalysisRequest request) {
        return orchestratorService.invokePipeline("observability-analysis-pipeline", request.toAgentContext());
    }
}