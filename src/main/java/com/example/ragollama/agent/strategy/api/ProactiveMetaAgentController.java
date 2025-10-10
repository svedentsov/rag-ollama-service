package com.example.ragollama.agent.strategy.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.incidentresponse.api.dto.IncidentAlertRequest;
import com.example.ragollama.agent.strategy.api.dto.MarketAnalysisRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Контроллер для проактивных, стратегических мета-агентов.
 */
@RestController
@RequestMapping("/api/v1/strategy")
@RequiredArgsConstructor
@Tag(name = "Strategic Meta-Agents")
public class ProactiveMetaAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Принимает алерт от внешней системы мониторинга и запускает
     * конвейер для автоматического расследования инцидента.
     *
     * @param request DTO с деталями алерта.
     * @return {@link Mono} с результатами анализа.
     */
    @PostMapping("/incident-analysis")
    @Operation(summary = "Принять алерт от системы мониторинга и запустить расследование")
    public Mono<List<AgentResult>> handleMonitoringAlert(@Valid @RequestBody IncidentAlertRequest request) {
        return orchestratorService.invoke("incident-response-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер для анализа рыночных возможностей на основе
     * информации о конкуренте.
     *
     * @param request DTO с URL сайта конкурента.
     * @return {@link Mono} с финальным отчетом о пробелах в функциональности.
     */
    @PostMapping("/analyze-market-opportunity")
    @Operation(summary = "Проанализировать конкурента и найти пробелы в функциональности")
    public Mono<List<AgentResult>> analyzeMarketOpportunity(@Valid @RequestBody MarketAnalysisRequest request) {
        return orchestratorService.invoke("market-opportunity-pipeline", request.toAgentContext());
    }
}
