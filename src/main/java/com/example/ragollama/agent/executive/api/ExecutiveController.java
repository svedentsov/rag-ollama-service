package com.example.ragollama.agent.executive.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.executive.api.dto.PortfolioStrategyRequest;
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
 * Контроллер для высокоуровневых, стратегических AI-агентов,
 * действующих на уровне всего портфеля проектов.
 */
@RestController
@RequestMapping("/api/v1/executive")
@RequiredArgsConstructor
@Tag(name = "Executive Agents", description = "API для получения стратегических инсайтов по всему портфелю")
public class ExecutiveController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает мета-агента "AI CTO" для анализа здоровья всех проектов и
     * формирования стратегического плана на следующий период.
     *
     * @param request DTO с периодом анализа.
     * @return {@link CompletableFuture} с отчетом, содержащим стратегические инициативы.
     */
    @PostMapping("/portfolio-strategy")
    @Operation(summary = "Сформировать стратегический план по портфелю проектов (AI CTO)")
    public CompletableFuture<List<AgentResult>> generatePortfolioStrategy(@Valid @RequestBody PortfolioStrategyRequest request) {
        return orchestratorService.invokePipeline("executive-governor-pipeline", request.toAgentContext());
    }
}
