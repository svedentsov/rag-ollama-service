package com.example.ragollama.optimization.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.api.dto.ConsistencyCheckRequest;
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
 * Контроллер для AI-агентов, отвечающих за проверку консистентности базы знаний.
 */
@RestController
@RequestMapping("/api/v1/consistency")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base Consistency", description = "API для аудита и проверки консистентности данных")
public class ConsistencyController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает полный конвейер для проверки одного утверждения на консистентность.
     *
     * @param request DTO с утверждением для проверки.
     * @return {@link Mono} с финальным отчетом о консистентности.
     */
    @PostMapping("/check")
    @Operation(summary = "Проверить утверждение на консистентность по всем источникам")
    public Mono<List<AgentResult>> checkConsistency(@Valid @RequestBody ConsistencyCheckRequest request) {
        return orchestratorService.invoke("consistency-check-pipeline", request.toAgentContext());
    }
}
