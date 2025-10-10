package com.example.ragollama.agent.executive.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.executive.api.dto.FinancialAnalysisRequest;
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
 * Контроллер для "Финансового Губернатора".
 */
@RestController
@RequestMapping("/api/v1/governance")
@RequiredArgsConstructor
@Tag(name = "Governance Agents")
public class FinancialGovernorController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает "AI CFO" для анализа затрат и ROI.
     *
     * @param request DTO с параметрами анализа.
     * @return {@link Mono} с финальным финансовым отчетом.
     */
    @PostMapping("/analyze-roi")
    @Operation(summary = "Проанализировать затраты и ROI инженерных инициатив (AI CFO)")
    public Mono<List<AgentResult>> analyzeFinancials(@Valid @RequestBody FinancialAnalysisRequest request) {
        return orchestratorService.invoke("financial-roi-analysis-pipeline", request.toAgentContext());
    }
}
