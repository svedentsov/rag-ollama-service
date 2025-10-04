package com.example.ragollama.agent.executive.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.executive.api.dto.PolicyGuardianRequest;
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
 * Контроллер для "Губернатора Политик и Безопасности".
 */
@RestController
@RequestMapping("/api/v1/governance")
@RequiredArgsConstructor
@Tag(name = "Governance Agents", description = "API для запуска комплексных проверок соответствия")
public class PolicyGuardianController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает полный конвейер для проверки изменений на соответствие всем
     * ключевым политикам компании.
     *
     * @param request DTO с Git-ссылками и текстами политик.
     * @return {@link CompletableFuture} с финальным отчетом и вердиктом.
     */
    @PostMapping("/enforce-policies")
    @Operation(summary = "Проверить изменения на соответствие всем политикам (Quality Gate)")
    public CompletableFuture<List<AgentResult>> enforcePolicies(@Valid @RequestBody PolicyGuardianRequest request) {
        return orchestratorService.invoke("policy-and-safety-governor-pipeline", request.toAgentContext());
    }
}
