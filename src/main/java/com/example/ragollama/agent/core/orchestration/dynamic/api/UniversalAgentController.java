package com.example.ragollama.agent.core.orchestration.dynamic.api;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.core.orchestration.dynamic.api.dto.DynamicTaskRequest;
import com.example.ragollama.agent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.agent.dynamic.PlanningAgentService;
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
 * Универсальный контроллер для динамического выполнения задач с помощью AI-агентов.
 */
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Dynamic Agent Orchestrator", description = "API для выполнения сложных задач с помощью динамических конвейеров")
public class UniversalAgentController {

    private final PlanningAgentService planningAgentService;
    private final DynamicPipelineExecutionService executionService;

    /**
     * Принимает задачу на естественном языке, строит и выполняет план по ее решению.
     * <p>
     * Этот эндпоинт является stateless и предназначен для выполнения
     * изолированных, одноразовых задач.
     *
     * @param request DTO с описанием задачи от пользователя.
     * @return {@link Mono} с финальным списком результатов от всех выполненных агентов.
     */
    @PostMapping("/execute-task")
    @Operation(summary = "Выполнить сложную задачу с помощью AI-планировщика",
            description = "Принимает задачу на естественном языке. LLM-планировщик создает план из доступных " +
                    "агентов-инструментов, который затем выполняется.")
    public Mono<List<AgentResult>> executeDynamicTask(@Valid @RequestBody DynamicTaskRequest request) {
        return planningAgentService.createPlan(request.taskDescription(), request.initialContext())
                .flatMap(plan -> executionService.executePlan(plan, request.toAgentContext(), null));
    }
}
