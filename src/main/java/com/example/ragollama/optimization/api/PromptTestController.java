package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.api.dto.PromptTestRequest;
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
 * Контроллер для AI-агента, управляющего A/B-тестированием промптов.
 */
@RestController
@RequestMapping("/api/v1/prompt-testing")
@RequiredArgsConstructor
@Tag(name = "Prompt Tester Agent", description = "API для проведения A/B-тестирования промптов")
public class PromptTestController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает A/B-тест для новой версии промпта.
     *
     * @param request DTO с именем промпта и его новым содержимым.
     * @return {@link Mono} с отчетом, содержащим сравнение метрик.
     */
    @PostMapping("/run")
    @Operation(summary = "Запустить A/B-тест для новой версии промпта")
    public Mono<List<AgentResult>> runPromptTest(@Valid @RequestBody PromptTestRequest request) {
        return orchestratorService.invoke("prompt-testing-pipeline", request.toAgentContext());
    }
}
