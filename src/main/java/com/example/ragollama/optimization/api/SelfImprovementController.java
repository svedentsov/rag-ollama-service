package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.api.dto.SelfImprovementRequest;
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

@RestController
@RequestMapping("/api/v1/self-improvement")
@RequiredArgsConstructor
@Tag(name = "Self-Improvement Agent", description = "API для запуска мета-анализа и улучшения AI-агентов")
public class SelfImprovementController {

    private final AgentOrchestratorService orchestratorService;

    @PostMapping("/analyze-and-suggest")
    @Operation(summary = "Проанализировать работу агентов и предложить улучшения промптов")
    public CompletableFuture<List<AgentResult>> analyzeAndSuggest(@Valid @RequestBody SelfImprovementRequest request) {
        return orchestratorService.invokePipeline("self-improvement-pipeline", request.toAgentContext());
    }
}
