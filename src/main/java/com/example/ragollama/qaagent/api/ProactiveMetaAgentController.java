package com.example.ragollama.qaagent.api;

import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.IncidentAlertRequest;
import com.example.ragollama.qaagent.api.dto.MarketAnalysisRequest;
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
 * Контроллеры для проактивных, стратегических мета-агентов.
 */
@RestController
@RequiredArgsConstructor
public class ProactiveMetaAgentController {

    private final AgentOrchestratorService orchestratorService;

    @Tag(name = "Strategic Meta-Agents")
    @PostMapping("/api/v1/webhooks/monitoring-alert")
    @Operation(summary = "Принять алерт от системы мониторинга и запустить расследование")
    public ResponseEntity<Void> handleMonitoringAlert(@Valid @RequestBody IncidentAlertRequest request) {
        orchestratorService.invokePipeline("incident-response-pipeline", request.toAgentContext());
        return ResponseEntity.accepted().build();
    }

    @Tag(name = "Strategic Meta-Agents")
    @PostMapping("/api/v1/agents/strategy/analyze-market-opportunity")
    @Operation(summary = "Проанализировать конкурента и найти пробелы в функциональности")
    public CompletableFuture<List<AgentResult>> analyzeMarketOpportunity(@Valid @RequestBody MarketAnalysisRequest request) {
        return orchestratorService.invokePipeline("market-opportunity-pipeline", request.toAgentContext());
    }
}
