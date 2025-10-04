package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.api.dto.TestDataRequest;
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
 * Контроллер для AI-агента, оркестрирующего операции с тестовыми данными.
 */
@RestController
@RequestMapping("/api/v1/test-data")
@RequiredArgsConstructor
@Tag(name = "Test DataOps Orchestrator", description = "API для интеллектуальной генерации тестовых данных")
public class TestDataOpsController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Принимает высокоуровневый запрос на тестовые данные и запускает
     * конвейер для их генерации.
     *
     * @param request DTO с целью и контекстом.
     * @return {@link CompletableFuture} с результатом, содержащим сгенерированные данные.
     */
    @PostMapping("/request")
    @Operation(summary = "Запросить тестовые данные на естественном языке")
    public CompletableFuture<List<AgentResult>> requestTestData(@Valid @RequestBody TestDataRequest request) {
        return orchestratorService.invoke("test-data-ops-pipeline", request.toAgentContext());
    }
}