package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.api.dto.ExperimentRequest;
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
 * Контроллер для AI-агентов, управляющих A/B-тестированием RAG-конфигураций.
 */
@RestController
@RequestMapping("/api/v1/experiments")
@RequiredArgsConstructor
@Tag(name = "Experiment Manager Agent", description = "API для проведения A/B-тестирования RAG-конфигураций")
public class ExperimentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает полный конвейер A/B-тестирования RAG-конфигураций.
     *
     * @param request DTO с описанием вариантов для тестирования.
     * @return {@link Mono} с финальным отчетом, определяющим лучшую конфигурацию.
     */
    @PostMapping("/run")
    @Operation(summary = "Запустить эксперимент по оценке RAG-конфигураций")
    public Mono<List<AgentResult>> runExperiment(@Valid @RequestBody ExperimentRequest request) {
        return orchestratorService.invoke("experiment-execution-pipeline", request.toAgentContext());
    }
}
