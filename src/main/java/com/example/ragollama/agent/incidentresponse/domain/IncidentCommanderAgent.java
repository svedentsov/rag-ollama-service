package com.example.ragollama.agent.incidentresponse.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.incidentresponse.model.IncidentReport;
import com.example.ragollama.optimization.WorkflowExecutionService;
import com.example.ragollama.optimization.WorkflowPlannerAgent;
import com.example.ragollama.shared.exception.ProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Мета-агент L4-уровня, выступающий в роли "AI Incident Commander".
 * <p>
 * Получив сводный отчет об инциденте, этот агент не выполняет действия
 * напрямую, а использует {@link WorkflowPlannerAgent} для построения
 * динамического плана реагирования (DAG). Затем он делегирует выполнение
 * этого плана {@link WorkflowExecutionService}.
 * <p>
 * Такой подход обеспечивает максимальную гибкость и расширяемость системы
 * реагирования на инциденты.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentCommanderAgent implements ToolAgent {

    private final WorkflowPlannerAgent plannerAgent;
    private final WorkflowExecutionService executionService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "incident-commander-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Планирует и запускает рабочий процесс реагирования на инцидент.";
    }

    /**
     * {@inheritDoc}
     *
     * @param context Контекст, который должен содержать `incidentReport`.
     * @return {@code true} если отчет об инциденте присутствует.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().get("incidentReport") instanceof IncidentReport;
    }

    /**
     * {@inheritDoc}
     *
     * @param context Контекст, содержащий {@link IncidentReport}.
     * @return {@link CompletableFuture} с результатом запуска рабочего процесса.
     * @throws ProcessingException если происходит ошибка при сериализации отчета.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        IncidentReport report = (IncidentReport) context.payload().get("incidentReport");
        try {
            String reportJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
            // Формулируем высокоуровневую цель для планировщика
            String goal = "Произошел инцидент. Проанализируй отчет и спланируй действия по сдерживанию: откати проблемное изменение и создай тикет P1 для расследования.";
            log.info("IncidentCommander: цель '{}'. Запуск планировщика...", goal);
            // 1. Создаем Workflow (DAG)
            return plannerAgent.createWorkflow(goal, Map.of("incidentReport", reportJson))
                    .flatMap(workflow -> {
                        // 2. Выполняем созданный Workflow
                        log.info("IncidentCommander: план из {} шагов получен. Запуск исполнителя...", workflow.size());
                        return executionService.executeWorkflow(workflow, context);
                    })
                    .map(results -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "План реагирования на инцидент успешно запущен.",
                            Map.of("incidentResponsePlan", results)
                    ))
                    .toFuture();
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчета об инциденте.", e));
        }
    }
}