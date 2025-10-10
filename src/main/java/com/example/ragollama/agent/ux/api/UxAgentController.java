package com.example.ragollama.agent.ux.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ux.api.dto.UserSimulationRequest;
import com.example.ragollama.agent.ux.api.dto.UxHeuristicsRequest;
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
 * Контроллер для AI-агентов, выполняющих анализ UI/UX.
 */
@RestController
@RequestMapping("/api/v1/agents/ux")
@RequiredArgsConstructor
@Tag(name = "UX/UI Agents", description = "API для анализа пользовательского интерфейса")
public class UxAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает агента для оценки HTML на соответствие эвристикам юзабилити Нильсена.
     *
     * @param request DTO с HTML-кодом.
     * @return {@link Mono} с результатом анализа.
     */
    @PostMapping("/evaluate-heuristics")
    @Operation(summary = "Оценить HTML на соответствие эвристикам юзабилити")
    public Mono<List<AgentResult>> evaluateUxHeuristics(@Valid @RequestBody UxHeuristicsRequest request) {
        return orchestratorService.invoke("ux-heuristics-evaluation-pipeline", request.toAgentContext());
    }

    /**
     * Запускает автономного AI-агента для симуляции поведения пользователя.
     *
     * @param request DTO с начальным URL и целью.
     * @return {@link Mono} с отчетом о выполненной симуляции.
     */
    @PostMapping("/simulate-user-behavior")
    @Operation(summary = "Запустить AI-агента для симуляции E2E-сценария")
    public Mono<List<AgentResult>> simulateUserBehavior(@Valid @RequestBody UserSimulationRequest request) {
        return orchestratorService.invoke("user-behavior-simulation-pipeline", request.toAgentContext());
    }
}
