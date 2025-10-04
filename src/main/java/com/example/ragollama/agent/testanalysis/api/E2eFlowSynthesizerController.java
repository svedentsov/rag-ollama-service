package com.example.ragollama.agent.testanalysis.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.testanalysis.api.dto.E2eFlowSynthesizerRequest;
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
 * Контроллер для AI-агента, синтезирующего E2E-тесты.
 */
@RestController
@RequestMapping("/api/v1/agents/e2e")
@RequiredArgsConstructor
@Tag(name = "E2E Flow Synthesizer Agent", description = "API для синтеза сквозных тестов")
public class E2eFlowSynthesizerController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Принимает описание пользовательского сценария и запускает конвейер для синтеза E2E-теста.
     *
     * @param request DTO с описанием сценария.
     * @return {@link CompletableFuture} с результатом, содержащим сгенерированный код.
     */
    @PostMapping("/synthesize")
    @Operation(summary = "Синтезировать E2E-тест из описания пользовательского сценария")
    public CompletableFuture<List<AgentResult>> synthesizeE2eFlow(@Valid @RequestBody E2eFlowSynthesizerRequest request) {
        return orchestratorService.invoke("e2e-flow-synthesis-pipeline", request.toAgentContext());
    }
}
