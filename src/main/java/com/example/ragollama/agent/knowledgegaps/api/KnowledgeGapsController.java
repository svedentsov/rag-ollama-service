package com.example.ragollama.agent.knowledgegaps.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.knowledgegaps.api.dto.KnowledgeGapAnalysisRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для AI-агентов, анализирующих пробелы в базе знаний.
 */
@RestController
@RequestMapping("/api/v1/knowledge-gaps")
@RequiredArgsConstructor
@Tag(name = "Knowledge Gaps API", description = "API для анализа и приоритизации тем для новой документации")
public class KnowledgeGapsController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает конвейер для анализа и кластеризации пробелов в знаниях.
     *
     * @param request DTO с параметрами анализа (например, период в днях).
     * @return {@link CompletableFuture} с отчетом, содержащим приоритизированные темы для документации.
     */
    @GetMapping("/analyze")
    @Operation(summary = "Проанализировать и сгруппировать пробелы в базе знаний")
    public CompletableFuture<List<AgentResult>> analyzeKnowledgeGaps(@Valid KnowledgeGapAnalysisRequest request) {
        return orchestratorService.invoke("knowledge-expansion-pipeline", request.toAgentContext());
    }
}
