package com.example.ragollama.agent.executive.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.executive.api.dto.ExecutiveDashboardRequest;
import com.example.ragollama.agent.executive.api.dto.PortfolioStrategyRequest;
import com.example.ragollama.agent.executive.model.ExecutiveDashboard;
import com.example.ragollama.optimization.ExecutiveDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Контроллер для высокоуровневых, стратегических AI-агентов ("Губернаторов").
 */
@RestController
@RequestMapping("/api/v1/executive")
@RequiredArgsConstructor
@Tag(name = "Executive Agents", description = "API для получения стратегических инсайтов по всему портфелю")
public class ExecutiveController {

    private final AgentOrchestratorService orchestratorService;
    private final ExecutiveDashboardService dashboardService;

    /**
     * Запускает мета-агента "AI CTO" для анализа технического здоровья всех проектов
     * и формирования стратегического плана по его улучшению.
     *
     * @param request DTO с периодом анализа.
     * @return {@link Mono} с результатом, содержащим отчет.
     */
    @PostMapping("/portfolio-strategy")
    @Operation(summary = "Сформировать стратегический план по портфелю проектов (AI CTO)",
            description = "Запускает конвейер 'executive-governor-pipeline' для анализа технического здоровья и рисков всех проектов.")
    public Mono<List<AgentResult>> generatePortfolioStrategy(@Valid @RequestBody PortfolioStrategyRequest request) {
        return orchestratorService.invoke("executive-governor-pipeline", request.toAgentContext());
    }

    /**
     * Собирает и возвращает полную, на 360 градусов, сводку о состоянии всех
     * инженерных процессов в виде единого дашборда для руководства.
     *
     * @param request DTO-объект, который Spring автоматически создает из параметров запроса.
     * @return {@link Mono}, который по завершении будет содержать
     * полностью собранный {@link ExecutiveDashboard}.
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Получить сводный дашборд для руководства (Executive Command Center)",
            description = "Асинхронно запускает всех AI-губернаторов и агрегирует их отчеты в единую панель управления.")
    public Mono<ExecutiveDashboard> getExecutiveDashboard(@Valid ExecutiveDashboardRequest request) {
        return dashboardService.generateDashboard(request.toAgentContext());
    }
}
