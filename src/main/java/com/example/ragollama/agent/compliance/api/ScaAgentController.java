package com.example.ragollama.agent.compliance.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.compliance.api.dto.ScaRequest;
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
 * Контроллер для AI-агентов, выполняющих анализ состава ПО (SCA).
 */
@RestController
@RequestMapping("/api/v1/agents/sca")
@RequiredArgsConstructor
@Tag(name = "SCA Agents", description = "API для анализа лицензий и уязвимостей зависимостей")
public class ScaAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает агента для анализа лицензий зависимостей на соответствие политике.
     *
     * @param request DTO с содержимым файла сборки и политикой лицензирования.
     * @return {@link Mono} с финальным отчетом о соответствии.
     */
    @PostMapping("/scan-licenses")
    @Operation(summary = "Проверить лицензии зависимостей на соответствие политике")
    public Mono<List<AgentResult>> scanLicenses(@Valid @RequestBody ScaRequest request) {
        return orchestratorService.invoke("sca-compliance-pipeline", request.toAgentContext());
    }
}
