package com.example.ragollama.agent.datacontract.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.datacontract.api.dto.DataContractRequest;
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
 * Контроллер для AI-агентов, обеспечивающих соблюдение контрактов данных.
 */
@RestController
@RequestMapping("/api/v1/agents/contracts")
@RequiredArgsConstructor
@Tag(name = "Data Contract Agents", description = "API для проверки совместимости DTO")
public class DataContractAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает агента для проверки обратной совместимости изменений в DTO.
     *
     * @param request DTO с Git-ссылками и путем к файлу DTO.
     * @return {@link CompletableFuture} с результатом анализа.
     */
    @PostMapping("/enforce")
    @Operation(summary = "Проверить DTO на наличие ломающих изменений")
    public CompletableFuture<List<AgentResult>> enforceDataContract(@Valid @RequestBody DataContractRequest request) {
        return orchestratorService.invoke("data-contract-enforcement-pipeline", request.toAgentContext());
    }
}
