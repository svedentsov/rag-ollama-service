package com.example.ragollama.agent.openapi.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.openapi.api.dto.SpecDriftAnalysisRequest;
import com.example.ragollama.agent.openapi.api.dto.SpecToTestRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Контроллер для управления AI-агентами, анализирующими OpenAPI спецификации.
 * <p>
 * Эта версия была упрощена. Эндпоинт `/query` удален, так как его
 * функциональность теперь унифицирована и доступна через
 * {@link com.example.ragollama.orchestration.api.UniversalController}.
 */
@RestController
@RequestMapping("/api/v1/agents/openapi")
@RequiredArgsConstructor
@Tag(name = "OpenAPI Agents", description = "API для семантического анализа OpenAPI спецификаций")
public class OpenApiAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает агента для обнаружения расхождений между спецификацией и реализацией.
     *
     * @param request DTO с источником OpenAPI спецификации.
     * @return {@link Mono} с отчетом о найденных расхождениях.
     */
    @PostMapping("/analyze-drift")
    @Operation(summary = "Обнаружить расхождения (drift) между спецификацией и кодом",
            description = "Запускает 'spec-drift-sentinel-pipeline', который сравнивает эндпоинты из " +
                    "спецификации с реально существующими в приложении.")
    public Mono<List<AgentResult>> analyzeSpecDrift(@Valid @RequestBody SpecDriftAnalysisRequest request) {
        return orchestratorService.invoke("spec-drift-sentinel-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для генерации кода API-теста на основе спецификации.
     *
     * @param request DTO с источником спецификации и целевым эндпоинтом.
     * @return {@link Mono} с результатом, содержащим сгенерированный Java-код.
     */
    @PostMapping("/generate-test")
    @Operation(summary = "Сгенерировать код API-теста из спецификации")
    public Mono<List<AgentResult>> generateTestFromSpec(@Valid @RequestBody SpecToTestRequest request) {
        return orchestratorService.invoke("spec-to-test-generation-pipeline", request.toAgentContext());
    }
}
