package com.example.ragollama.qaagent.api;

import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.ComplianceEvidenceRequest;
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
 * Контроллер для AI-агентов, выполняющих задачи по обеспечению соответствия (Compliance).
 */
@RestController
@RequestMapping("/api/v1/agents/compliance")
@RequiredArgsConstructor
@Tag(name = "Compliance Agents", description = "API для автоматизации сбора доказательств для аудитов")
public class ComplianceAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает полный конвейер для сбора доказательств по жизненному циклу разработки.
     *
     * @param request DTO с Git-ссылками, определяющими объем изменений.
     * @return {@link CompletableFuture} с финальным, агрегированным отчетом в формате Markdown.
     */
    @PostMapping("/collect-evidence")
    @Operation(summary = "Собрать доказательства для аудита соответствия по изменениям в коде")
    public CompletableFuture<List<AgentResult>> collectComplianceEvidence(@Valid @RequestBody ComplianceEvidenceRequest request) {
        return orchestratorService.invokePipeline("compliance-evidence-pipeline", request.toAgentContext());
    }
}
