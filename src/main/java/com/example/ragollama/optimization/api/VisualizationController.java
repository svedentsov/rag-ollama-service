package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.api.dto.VisualizationRequest;
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
@RequestMapping("/api/v1/visualizations")
@RequiredArgsConstructor
@Tag(name = "Visualization Agent", description = "API для генерации графиков и диаграмм из данных")
public class VisualizationController {

    private final AgentOrchestratorService orchestratorService;

    @PostMapping("/generate")
    @Operation(summary = "Сгенерировать код визуализации из данных")
    public CompletableFuture<List<AgentResult>> generateVisualization(@Valid @RequestBody VisualizationRequest request) {
        return orchestratorService.invokePipeline("visualization-pipeline", request.toAgentContext());
    }
}
