package com.example.ragollama.agent.coverage.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.coverage.api.dto.CoverageAuditRequest;
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
 * Контроллер для AI-агентов, связанных с анализом покрытия кода.
 */
@RestController
@RequestMapping("/api/v1/agents/coverage")
@RequiredArgsConstructor
@Tag(name = "Test Coverage Agents", description = "API для аудита тестового покрытия")
public class CoverageController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает полный конвейер аудита тестового покрытия для изменений.
     *
     * @param request DTO с Git-ссылками и JaCoCo-отчетом.
     * @return {@link CompletableFuture} со структурированным отчетом о рисках.
     */
    @PostMapping("/audit")
    @Operation(summary = "Провести аудит тестового покрытия для изменений в коде")
    public CompletableFuture<List<AgentResult>> auditCoverage(@Valid @RequestBody CoverageAuditRequest request) {
        return orchestratorService.invokePipeline("coverage-audit-pipeline", request.toAgentContext());
    }
}
