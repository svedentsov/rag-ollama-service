package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.api.dto.ResourceAllocationRequest;
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
 * Контроллер для AI-агентов, отвечающих за анализ и оптимизацию
 * выделенных инфраструктурных ресурсов.
 */
@RestController
@RequestMapping("/api/v1/resource-allocation")
@RequiredArgsConstructor
@Tag(name = "Resource Allocation Agent", description = "API для анализа и оптимизации инфраструктурных ресурсов")
public class ResourceAllocationController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает полный конвейер для анализа использования ресурсов сервиса
     * и генерации рекомендаций по их оптимизации.
     *
     * @param request DTO с именем сервиса и его текущей конфигурацией ресурсов.
     * @return {@link CompletableFuture} с финальным отчетом, содержащим
     * рекомендуемую конфигурацию.
     */
    @PostMapping("/analyze")
    @Operation(summary = "Проанализировать использование ресурсов и предложить оптимизацию",
            description = "Запускает 'resource-allocation-pipeline', который симулирует сбор исторических " +
                    "метрик и использует AI для предложения более оптимальной конфигурации CPU/Memory.")
    public CompletableFuture<List<AgentResult>> analyzeAndSuggest(@Valid @RequestBody ResourceAllocationRequest request) {
        return orchestratorService.invokePipeline("resource-allocation-pipeline", request.toAgentContext());
    }
}
