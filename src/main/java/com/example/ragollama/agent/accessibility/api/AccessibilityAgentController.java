package com.example.ragollama.agent.accessibility.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditRequest;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditResponse;
import com.example.ragollama.agent.accessibility.mappers.AccessibilityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для AI-агента, выполняющего аудит доступности (a11y).
 * <p>
 * Этот контроллер строго следует принципам Clean Architecture:
 * <ul>
 *     <li>Он работает исключительно с DTO (Data Transfer Objects) для изоляции
 *         внутренней доменной модели от внешнего мира.</li>
 *     <li>Он делегирует всю бизнес-логику сервисному слою
 *         ({@link AgentOrchestratorService}).</li>
 *     <li>Он не содержит никакой логики, кроме валидации, маппинга и вызова сервиса.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/agents/accessibility")
@RequiredArgsConstructor
@Tag(name = "Accessibility Agent (a11y)", description = "API для аудита доступности UI")
public class AccessibilityAgentController {

    private final AgentOrchestratorService orchestratorService;
    private final AccessibilityMapper accessibilityMapper;

    /**
     * Принимает HTML-код страницы и запускает конвейер для его анализа на предмет нарушений доступности.
     *
     * @param request DTO с HTML-кодом.
     * @return {@link CompletableFuture} с результатом работы агента, преобразованным в DTO ответа.
     */
    @PostMapping("/audit")
    @Operation(summary = "Провести аудит доступности (a11y) для HTML-кода")
    public CompletableFuture<AccessibilityAuditResponse> auditAccessibility(@Valid @RequestBody AccessibilityAuditRequest request) {
        return orchestratorService.invokePipeline("accessibility-audit-pipeline", request.toAgentContext())
                .thenApply(accessibilityMapper::toResponseDto);
    }
}
