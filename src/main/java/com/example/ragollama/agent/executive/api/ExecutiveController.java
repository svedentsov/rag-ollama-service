package com.example.ragollama.agent.executive.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.executive.api.dto.ExecutiveDashboardRequest;
import com.example.ragollama.agent.executive.api.dto.PortfolioStrategyRequest;
import com.example.ragollama.agent.executive.domain.ExecutiveDashboardService;
import com.example.ragollama.agent.executive.model.ExecutiveDashboard;
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
 * Контроллер для высокоуровневых, стратегических AI-агентов ("Губернаторов"),
 * действующих на уровне всего портфеля проектов.
 * <p>
 * Этот API является точкой входа для получения самых комплексных и ценных
 * аналитических отчетов, предназначенных для принятия управленческих решений.
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
     * @return {@link CompletableFuture} с результатом, содержащим отчет
     * {@link com.example.ragollama.agent.strategy.model.PortfolioStrategyReport}.
     */
    @PostMapping("/portfolio-strategy")
    @Operation(summary = "Сформировать стратегический план по портфелю проектов (AI CTO)",
            description = "Запускает конвейер 'executive-governor-pipeline' для анализа технического здоровья и рисков всех проектов.")
    public CompletableFuture<List<AgentResult>> generatePortfolioStrategy(@Valid @RequestBody PortfolioStrategyRequest request) {
        return orchestratorService.invokePipeline("executive-governor-pipeline", request.toAgentContext());
    }

    /**
     * Собирает и возвращает полную, на 360 градусов, сводку о состоянии всех
     * инженерных процессов в виде единого дашборда для руководства.
     * <p>
     * Этот эндпоинт асинхронно и параллельно запускает **всех** AI-губернаторов
     * (по здоровью, скорости, продукту, финансам и архитектуре), агрегирует
     * их финальные отчеты и представляет в виде единого, целостного объекта.
     *
     * @param request DTO со всеми параметрами, необходимыми для полного анализа.
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * полностью собранный {@link ExecutiveDashboard}.
     */
    @PostMapping("/dashboard")
    @Operation(summary = "Получить сводный дашборд для руководства (Executive Command Center)",
            description = "Асинхронно запускает всех AI-губернаторов и агрегирует их отчеты в единую панель управления.")
    public CompletableFuture<ExecutiveDashboard> getExecutiveDashboard(@Valid @RequestBody ExecutiveDashboardRequest request) {
        return dashboardService.generateDashboard(request.toAgentContext());
    }
}
