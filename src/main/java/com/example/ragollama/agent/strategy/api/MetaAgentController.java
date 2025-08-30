package com.example.ragollama.agent.strategy.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.strategy.api.dto.MetaAgentRequest;
import com.example.ragollama.agent.strategy.api.dto.SprintPlanningRequest;
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
 * Контроллер для высокоуровневых, стратегических мета-агентов.
 * <p>
 * Предоставляет API для запуска сложных аналитических конвейеров, которые
 * имитируют принятие решений на уровне тимлида или продакт-менеджера.
 */
@RestController
@RequestMapping("/api/v1/agents/strategy")
@RequiredArgsConstructor
@Tag(name = "Strategic Meta-Agents", description = "API для запуска мета-агентов, принимающих стратегические решения")
public class MetaAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает мета-агента "AI Tech Lead" для поиска точек технического долга
     * и формирования предложений по рефакторингу.
     *
     * @param request DTO с периодом анализа.
     * @return {@link CompletableFuture} с отчетом, содержащим стратегию рефакторинга.
     */
    @PostMapping("/plan-refactoring")
    @Operation(summary = "Сформировать стратегию рефакторинга (AI Tech Lead)")
    public CompletableFuture<List<AgentResult>> planRefactoring(@Valid @RequestBody MetaAgentRequest request) {
        return orchestratorService.invokePipeline("strategic-refactoring-pipeline", request.toAgentContext());
    }

    /**
     * Запускает мета-агента "AI Product Manager" для формирования плана на спринт.
     *
     * @param request DTO с периодом анализа.
     * @return {@link CompletableFuture} с отчетом, содержащим цель и бэклог спринта.
     */
    @PostMapping("/plan-sprint")
    @Operation(summary = "Сформировать план на спринт (AI Product Manager)")
    public CompletableFuture<List<AgentResult>> planSprint(@Valid @RequestBody SprintPlanningRequest request) {
        return orchestratorService.invokePipeline("sprint-planning-pipeline", request.toAgentContext());
    }
}
