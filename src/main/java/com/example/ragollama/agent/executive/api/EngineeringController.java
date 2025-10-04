package com.example.ragollama.agent.executive.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.executive.api.dto.EngineeringVelocityRequest;
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
 * Контроллер для AI-агентов, анализирующих инженерные процессы и производительность.
 */
@RestController
@RequestMapping("/api/v1/engineering")
@RequiredArgsConstructor
@Tag(name = "Engineering Intelligence Agents", description = "API для анализа и оптимизации SDLC")
public class EngineeringController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает "Губернатора Инженерной Производительности" для анализа SDLC.
     *
     * @param request DTO с параметрами анализа.
     * @return {@link CompletableFuture} с финальным отчетом об эффективности.
     */
    @PostMapping("/analyze-velocity")
    @Operation(summary = "Проанализировать производительность SDLC (AI VP of Engineering)")
    public CompletableFuture<List<AgentResult>> analyzeEngineeringVelocity(@Valid @RequestBody EngineeringVelocityRequest request) {
        return orchestratorService.invoke("engineering-velocity-pipeline", request.toAgentContext());
    }
}
