package com.example.ragollama.agent.architecture.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.architecture.api.dto.ArchConsistencyRequest;
import com.example.ragollama.agent.architecture.api.dto.ArchitecturalGuidanceRequest;
import com.example.ragollama.agent.architecture.api.dto.VisualizationRequest;
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
 * Контроллер для AI-агентов, выполняющих архитектурный надзор и анализ.
 */
@RestController
@RequestMapping("/api/v1/agents/architecture")
@RequiredArgsConstructor
@Tag(name = "Architecture Agents", description = "API для проверки архитектурной консистентности и получения ревью")
public class ArchitectureAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает агента для проверки соответствия измененного кода эталонной архитектуре.
     *
     * @param request DTO с описанием архитектуры и списком измененных файлов.
     * @return {@link CompletableFuture} с результатом анализа.
     */
    @PostMapping("/check-consistency")
    @Operation(summary = "Проверить измененный код на соответствие эталонной архитектуре")
    public CompletableFuture<List<AgentResult>> checkArchitectureConsistency(@Valid @RequestBody ArchConsistencyRequest request) {
        return orchestratorService.invokePipeline("arch-consistency-mapping-pipeline", request.toAgentContext());
    }

    /**
     * Запускает мета-агента "AI Architecture Governor" для проведения полного ревью.
     *
     * @param request DTO с Git-ссылками и политиками.
     * @return {@link CompletableFuture} с финальным отчетом-ревью.
     */
    @PostMapping("/full-review")
    @Operation(summary = "Получить полное архитектурное ревью для изменений (AI Governor)")
    public CompletableFuture<List<AgentResult>> getFullArchitecturalReview(@Valid @RequestBody ArchitecturalGuidanceRequest request) {
        return orchestratorService.invokePipeline("architectural-guardian-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер для анализа зависимостей и генерации диаграммы.
     *
     * @param request DTO с Git-ссылками, определяющими диапазон анализа.
     * @return {@link CompletableFuture} с результатом, содержащим код диаграммы в формате Mermaid.js.
     */
    @PostMapping("/visualize")
    @Operation(summary = "Сгенерировать диаграмму зависимостей для изменений")
    public CompletableFuture<List<AgentResult>> visualizeDependencies(@Valid @RequestBody VisualizationRequest request) {
        return orchestratorService.invokePipeline("architecture-visualization-pipeline", request.toAgentContext());
    }
}
