package com.example.ragollama.qaagent.api;

import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.OpenApiQueryRequest;
import com.example.ragollama.qaagent.api.dto.SpecDriftAnalysisRequest;
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
 * Контроллер для управления AI-агентами, анализирующими OpenAPI спецификации.
 */
@RestController
@RequestMapping("/api/v1/agents/openapi")
@RequiredArgsConstructor
@Tag(name = "OpenAPI Agents", description = "API для семантического анализа OpenAPI спецификаций")
public class OpenApiAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает RAG-конвейер для ответа на вопрос по OpenAPI спецификации.
     *
     * @param request DTO с URL или содержимым спецификации и вопросом пользователя.
     * @return {@link CompletableFuture} с ответом, сгенерированным LLM.
     */
    @PostMapping("/query")
    @Operation(summary = "Задать вопрос по OpenAPI спецификации",
            description = "Запускает 'openapi-pipeline', который динамически создает RAG-конвейер " +
                    "на основе предоставленной спецификации для ответа на вопрос.")
    public CompletableFuture<List<AgentResult>> querySpec(@Valid @RequestBody OpenApiQueryRequest request) {
        return orchestratorService.invokePipeline("openapi-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для обнаружения расхождений между спецификацией и реализацией.
     *
     * @param request DTO с источником OpenAPI спецификации.
     * @return {@link CompletableFuture} с отчетом о найденных расхождениях.
     */
    @PostMapping("/analyze-drift")
    @Operation(summary = "Обнаружить расхождения (drift) между спецификацией и кодом",
            description = "Запускает 'spec-drift-sentinel-pipeline', который сравнивает эндпоинты из " +
                    "спецификации с реально существующими в приложении.")
    public CompletableFuture<List<AgentResult>> analyzeSpecDrift(@Valid @RequestBody SpecDriftAnalysisRequest request) {
        return orchestratorService.invokePipeline("spec-drift-sentinel-pipeline", request.toAgentContext());
    }
}
