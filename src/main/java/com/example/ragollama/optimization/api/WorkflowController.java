package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.WorkflowExecutionService;
import com.example.ragollama.optimization.WorkflowPlannerAgent;
import com.example.ragollama.optimization.api.dto.WorkflowRequest;
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
 * Контроллер для AI-агента, оркестрирующего сложные, нелинейные рабочие процессы.
 * <p>
 * Этот API предоставляет самый высокий уровень абстракции, позволяя запускать
 * многошаговые процессы с параллельными и условными ветвлениями,
 * описанными одной командой на естественном языке.
 */
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflow Orchestrator", description = "API для выполнения сложных, нелинейных рабочих процессов (DAGs)")
public class WorkflowController {

    private final WorkflowPlannerAgent plannerAgent;
    private final WorkflowExecutionService executionService;

    /**
     * Принимает высокоуровневую цель, строит граф выполнения (DAG) и асинхронно выполняет его.
     *
     * @param request DTO с описанием цели и начальным контекстом.
     * @return {@link Mono} с финальным списком результатов всех выполненных агентов.
     */
    @PostMapping("/execute")
    @Operation(summary = "Выполнить сложный рабочий процесс (workflow) по его описанию",
            description = "Принимает задачу на естественном языке. AI-планировщик строит граф зависимостей (DAG) " +
                    "из доступных агентов, который затем выполняется с учетом параллелизма.")
    public Mono<List<AgentResult>> executeWorkflow(@Valid @RequestBody WorkflowRequest request) {
        return plannerAgent.createWorkflow(request.goal(), request.toAgentContext().payload())
                .flatMap(workflow -> executionService.executeWorkflow(workflow, request.toAgentContext()));
    }
}