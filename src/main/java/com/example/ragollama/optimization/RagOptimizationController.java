package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для запуска AI-агентов, отвечающих за оптимизацию RAG-системы.
 */
@RestController
@RequestMapping("/api/v1/optimizers")
@RequiredArgsConstructor
@Tag(name = "Optimization Agents", description = "API для запуска анализа и оптимизации RAG-конвейера")
public class RagOptimizationController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает мета-агента для анализа производительности RAG и генерации предложений по оптимизации.
     *
     * @return {@link Mono} с отчетом, содержащим рекомендации.
     */
    @PostMapping("/rag/suggest-improvements")
    @Operation(summary = "Проанализировать производительность RAG и предложить улучшения")
    public Mono<List<AgentResult>> suggestRagImprovements() {
        return orchestratorService.invoke("rag-optimizer-pipeline", new com.example.ragollama.agent.AgentContext(Map.of()));
    }
}
