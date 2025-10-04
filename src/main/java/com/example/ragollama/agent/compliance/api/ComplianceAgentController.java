package com.example.ragollama.agent.compliance.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.compliance.api.dto.ComplianceEvidenceRequest;
import com.example.ragollama.agent.compliance.api.dto.PrivacyCheckRequest;
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
 * Контроллер для AI-агентов, выполняющих задачи аудита и комплаенса.
 */
@RestController
@RequestMapping("/api/v1/agents/compliance")
@RequiredArgsConstructor
@Tag(name = "Compliance Agents", description = "API для автоматизации сбора доказательств и проверок")
public class ComplianceAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает агента для проверки измененного кода на соответствие политикам конфиденциальности.
     *
     * @param request DTO с политикой и списком измененных файлов.
     * @return {@link CompletableFuture} с результатом анализа.
     */
    @PostMapping("/check-privacy")
    @Operation(summary = "Проверить код на соответствие политикам конфиденциальности")
    public CompletableFuture<List<AgentResult>> checkPrivacyCompliance(@Valid @RequestBody PrivacyCheckRequest request) {
        return orchestratorService.invoke("privacy-compliance-check-pipeline", request.toAgentContext());
    }

    /**
     * Запускает полный конвейер для сбора доказательств и генерации аудиторского отчета.
     *
     * @param request DTO с Git-ссылками, определяющими диапазон аудита.
     * @return {@link CompletableFuture} с финальным отчетом в формате Markdown.
     */
    @PostMapping("/gather-evidence")
    @Operation(summary = "Собрать доказательства для аудита соответствия")
    public CompletableFuture<List<AgentResult>> gatherComplianceEvidence(@Valid @RequestBody ComplianceEvidenceRequest request) {
        return orchestratorService.invoke("compliance-evidence-gathering-pipeline", request.toAgentContext());
    }
}
