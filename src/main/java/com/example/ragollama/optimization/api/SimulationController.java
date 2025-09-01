package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.WorkflowExecutionService;
import com.example.ragollama.optimization.WorkflowPlannerAgent;
import com.example.ragollama.optimization.api.dto.SimulationRequest;
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

@RestController
@RequestMapping("/api/v1/simulations")
@RequiredArgsConstructor
@Tag(name = "Simulation Agent", description = "API для запуска и анализа гипотетических сценариев")
public class SimulationController {

    private final WorkflowPlannerAgent plannerAgent;
    private final WorkflowExecutionService executionService;

    @PostMapping("/run")
    @Operation(summary = "Запустить симуляцию и получить аналитический отчет")
    public Mono<List<AgentResult>> runSimulation(@Valid @RequestBody SimulationRequest request) {
        return plannerAgent.createWorkflow(request.scenario(), request.parameters())
                .flatMap(workflow -> executionService.executeWorkflow(workflow, request.toAgentContext()));
    }
}
