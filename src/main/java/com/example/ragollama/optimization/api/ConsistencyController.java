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

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
     * @return {@link CompletableFuture} с финальным отчетом о консистентности.
     */
    @PostMapping("/check")
    @Operation(summary = "Проверить утверждение на консистентность по всем источникам",
            description = "Запускает 'consistency-check-pipeline', который сначала собирает " +
                    "доказательства из разных источников, а затем сравнивает их на предмет противоречий.")
    public CompletableFuture<List<AgentResult>> checkConsistency(@Valid @RequestBody ConsistencyCheckRequest request) {
        return orchestratorService.invokePipeline("consistency-check-pipeline", request.toAgentContext());
    }
}
