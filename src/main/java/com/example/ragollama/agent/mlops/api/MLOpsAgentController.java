package com.example.ragollama.agent.mlops.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.mlops.api.dto.DriftDetectionRequest;
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
 * Контроллер для AI-агентов, выполняющих задачи MLOps.
 */
@RestController
@RequestMapping("/api/v1/agents/mlops")
@RequiredArgsConstructor
@Tag(name = "MLOps Agents", description = "API для мониторинга ML-моделей")
public class MLOpsAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает агента для анализа дрейфа признаков между двумя наборами данных.
     *
     * @param request DTO с эталонным и текущим наборами данных.
     * @return {@link Mono} с финальным отчетом о дрейфе.
     */
    @PostMapping("/detect-feature-drift")
    @Operation(summary = "Обнаружить дрейф признаков (Feature Drift) между двумя наборами данных")
    public Mono<List<AgentResult>> detectFeatureDrift(@Valid @RequestBody DriftDetectionRequest request) {
        return orchestratorService.invoke("ml-feature-drift-pipeline", request.toAgentContext());
    }
}
