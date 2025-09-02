package com.example.ragollama.agent.executive.api;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.executive.api.dto.ProductStrategyRequest;
import com.example.ragollama.optimization.WorkflowExecutionService;
import com.example.ragollama.optimization.WorkflowPlannerAgent;
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
 * Контроллер для AI-агента "Product Portfolio Strategist".
 */
@RestController
@RequestMapping("/api/v1/strategy")
@RequiredArgsConstructor
@Tag(name = "Strategic Meta-Agents")
public class ProductStrategyController {

    private final WorkflowPlannerAgent plannerAgent;
    private final WorkflowExecutionService executionService;

    /**
     * Запускает "Продуктового Стратега" для анализа рынка и фидбэка.
     *
     * @param request DTO с URL конкурентов и периодом анализа.
     * @return {@link Mono} с финальным стратегическим отчетом.
     */
    @PostMapping("/product-portfolio")
    @Operation(summary = "Сформировать продуктовую стратегию на основе анализа рынка и фидбэка (AI CPO)")
    public Mono<List<AgentResult>> generateProductStrategy(@Valid @RequestBody ProductStrategyRequest request) {
        String goal = String.format(
                "Провести полный анализ продуктового портфеля. " +
                        "Сначала проанализировать обратную связь от пользователей за последние %d дней. " +
                        "Параллельно, для каждого из следующих конкурентов [%s], провести анализ рыночных возможностей. " +
                        "Наконец, агрегировать все результаты и сформировать единую продуктовую стратегию.",
                request.analysisPeriodDays(), String.join(", ", request.competitorUrls())
        );

        return plannerAgent.createWorkflow(goal, request.toAgentContext().payload())
                .flatMap(workflow -> executionService.executeWorkflow(workflow, request.toAgentContext()));
    }
}
