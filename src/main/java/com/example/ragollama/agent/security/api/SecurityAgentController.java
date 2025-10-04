package com.example.ragollama.agent.security.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.security.api.dto.SecurityScanRequest;
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
 * Контроллер для AI-агентов, выполняющих задачи по обеспечению безопасности.
 */
@RestController
@RequestMapping("/api/v1/agents/security")
@RequiredArgsConstructor
@Tag(name = "Security Agents", description = "API для оркестрации сканирований безопасности")
public class SecurityAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает полный, многоэтапный конвейер аудита безопасности для изменений в коде.
     *
     * @param request DTO с Git-ссылками и опциональными логами.
     * @return {@link CompletableFuture} с финальным, агрегированным отчетом о безопасности.
     */
    @PostMapping("/full-scan")
    @Operation(summary = "Провести полный аудит безопасности (SAST, RBAC, PII)")
    public CompletableFuture<List<AgentResult>> runFullSecurityScan(@Valid @RequestBody SecurityScanRequest request) {
        return orchestratorService.invoke("full-security-audit-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер для генерации Fuzzing-тестов, нацеленных на проверку RBAC и IDOR.
     *
     * @param request DTO с Git-ссылками, определяющими область анализа.
     * @return {@link CompletableFuture} с результатом, содержащим сгенерированный код тестов.
     */
    @PostMapping("/generate-rbac-fuzz-tests")
    @Operation(summary = "Сгенерировать Fuzzing-тесты для проверки RBAC",
            description = "Анализирует изменения в коде, извлекает правила RBAC, генерирует атакующие персоны и создает код теста для симуляции IDOR-атаки.")
    public CompletableFuture<List<AgentResult>> generateRbacFuzzTests(@Valid @RequestBody SecurityScanRequest request) {
        return orchestratorService.invoke("rbac-fuzzing-pipeline", request.toAgentContext());
    }
}
