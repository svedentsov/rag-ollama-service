package com.example.ragollama.agent.strategy.api.ProactiveMetaAgentController;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.incidentresponse.api.dto.IncidentAlertRequest;
import com.example.ragollama.agent.strategy.api.dto.MarketAnalysisRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для проактивных, стратегических мета-агентов.
 * <p>
 * Предоставляет эндпоинты, которые запускают сложные, многошаговые
 * конвейеры для решения стратегических задач, таких как анализ
 * инцидентов или исследование рынка.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Strategic Meta-Agents")
public class ProactiveMetaAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Принимает алерт от внешней системы мониторинга и запускает
     * конвейер для автоматического расследования инцидента.
     *
     * @param request DTO с деталями алерта.
     * @return {@link ResponseEntity} со статусом 202 (Accepted), подтверждающий
     *         прием задачи в асинхронную обработку.
     */
    @PostMapping("/api/v1/webhooks/monitoring-alert")
    @Operation(summary = "Принять алерт от системы мониторинга и запустить расследование")
    public ResponseEntity<Void> handleMonitoringAlert(@Valid @RequestBody IncidentAlertRequest request) {
        orchestratorService.invokePipeline("incident-response-pipeline", request.toAgentContext());
        return ResponseEntity.accepted().build();
    }

    /**
     * Запускает конвейер для анализа рыночных возможностей на основе
     * информации о конкуренте.
     *
     * @param request DTO с URL сайта конкурента.
     * @return {@link CompletableFuture} с финальным отчетом о пробелах в функциональности.
     */
    @PostMapping("/api/v1/agents/strategy/analyze-market-opportunity")
    @Operation(summary = "Проанализировать конкурента и найти пробелы в функциональности")
    public CompletableFuture<List<AgentResult>> analyzeMarketOpportunity(@Valid @RequestBody MarketAnalysisRequest request) {
        return orchestratorService.invokePipeline("market-opportunity-pipeline", request.toAgentContext());
    }
}
