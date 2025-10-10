package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.api.dto.PrioritizationRequest;
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
 * Контроллер для AI-агента, отвечающего за автоматическую приоритизацию задач.
 */
@RestController
@RequestMapping("/api/v1/prioritization")
@RequiredArgsConstructor
@Tag(name = "Prioritization Agent", description = "API для автоматической приоритизации задач")
public class PrioritizationController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает полный конвейер для анализа состояния проекта и генерации
     * приоритизированного бэклога на спринт.
     *
     * @param request DTO с бизнес-целью спринта и периодом для анализа.
     * @return {@link Mono} с финальным отчетом, содержащим
     * приоритизированный список задач.
     */
    @PostMapping("/plan-sprint")
    @Operation(summary = "Сгенерировать приоритизированный план на спринт")
    public Mono<List<AgentResult>> prioritizeBacklog(@Valid @RequestBody PrioritizationRequest request) {
        return orchestratorService.invoke("prioritization-pipeline", request.toAgentContext());
    }
}
