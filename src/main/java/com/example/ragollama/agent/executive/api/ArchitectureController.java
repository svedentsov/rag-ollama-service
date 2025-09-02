package com.example.ragollama.agent.executive.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.executive.api.dto.ArchitecturalHealthRequest;
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
 * Контроллер для "Губернатора Архитектурной Эволюции".
 */
@RestController
@RequestMapping("/api/v1/architecture")
@RequiredArgsConstructor
@Tag(name = "Governance Agents")
public class ArchitectureController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает "AI Chief Architect" для анализа долгосрочного здоровья архитектуры.
     *
     * @param request Пустой DTO-заглушка.
     * @return {@link CompletableFuture} с финальным стратегическим отчетом.
     */
    @PostMapping("/analyze-health")
    @Operation(summary = "Проанализировать долгосрочное здоровье архитектуры (AI Chief Architect)")
    public CompletableFuture<List<AgentResult>> analyzeArchitecturalHealth(@Valid @RequestBody ArchitecturalHealthRequest request) {
        return orchestratorService.invokePipeline("architectural-evolution-pipeline", request.toAgentContext());
    }
}
