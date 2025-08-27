package com.example.ragollama.qaagent.api;

import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.AccessibilityAuditRequest;
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
 * Контроллер для AI-агента, выполняющего аудит доступности (a11y).
 */
@RestController
@RequestMapping("/api/v1/agents/accessibility")
@RequiredArgsConstructor
@Tag(name = "Accessibility Agent (a11y)", description = "API для аудита доступности UI")
public class AccessibilityAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Принимает HTML-код страницы и запускает конвейер для его анализа на предмет нарушений доступности.
     *
     * @param request DTO с HTML-кодом.
     * @return {@link CompletableFuture} с результатом работы агента, содержащим полный отчет.
     */
    @PostMapping("/audit")
    @Operation(summary = "Провести аудит доступности (a11y) для HTML-кода")
    public CompletableFuture<List<AgentResult>> auditAccessibility(@Valid @RequestBody AccessibilityAuditRequest request) {
        return orchestratorService.invokePipeline("accessibility-audit-pipeline", request.toAgentContext());
    }
}
