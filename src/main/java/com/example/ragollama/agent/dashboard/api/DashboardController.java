package com.example.ragollama.agent.dashboard.api;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.dashboard.model.QaDashboard;
import com.example.ragollama.agent.strategy.domain.FederatedInsightsAgent;
import com.example.ragollama.optimization.QaCommandCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Контроллер, предоставляющий доступ к сводным дашбордам состояния QA.
 * <p>
 * Является единой точкой входа для получения холистической картины
 * качества, рисков и технического долга как для отдельных изменений,
 * так и для всей совокупности отслеживаемых проектов.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "QA Command Center", description = "API для получения сводных аналитических дашбордов")
public class DashboardController {

    private final QaCommandCenterService commandCenterService;
    private final FederatedInsightsAgent federatedInsightsAgent;

    /**
     * Собирает и возвращает полную сводку о состоянии QA-процессов для
     * анализа изменений между двумя Git-ссылками.
     *
     * @param baseRef Исходная Git-ссылка для анализа (например, 'main').
     * @param headRef Конечная Git-ссылка для анализа (например, 'feature/new-logic').
     * @return {@link Mono}, который по завершении будет содержать
     * полностью собранный дашборд.
     */
    @GetMapping("/qa-overview")
    @Operation(summary = "Получить сводный QA-дашборд для изменений")
    public Mono<QaDashboard> getQaOverview(
            @RequestParam(defaultValue = "main") String baseRef,
            @RequestParam String headRef) {
        return commandCenterService.generateDashboard(baseRef, headRef);
    }

    /**
     * Запускает федеративный анализ по всем проектам и возвращает стратегический отчет.
     *
     * @return {@link Mono}, который по завершении будет содержать
     * отчет с выводами и рекомендациями от AI-стратега.
     */
    @GetMapping("/federated-overview")
    @Operation(summary = "Получить сводный стратегический отчет по всем проектам (Federated)")
    public Mono<AgentResult> getFederatedOverview() {
        return federatedInsightsAgent.execute(new AgentContext(Map.of()));
    }
}
