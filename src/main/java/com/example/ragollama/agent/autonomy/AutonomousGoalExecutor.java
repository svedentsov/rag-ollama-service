package com.example.ragollama.agent.autonomy;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.optimization.WorkflowExecutionService;
import com.example.ragollama.optimization.WorkflowPlannerAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис-фасад, предоставляющий единую точку входа для запуска
 * автономных, высокоуровневых задач.
 * <p>
 * Этот сервис является ключевым элементом в рефакторинге L5-агентов. Он инкапсулирует
 * взаимодействие с L4-уровнем (планировщик и исполнитель), позволяя
 * L5-агентам работать в декларативном стиле, просто формулируя цель.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutonomousGoalExecutor {

    private final WorkflowPlannerAgent plannerAgent;
    private final WorkflowExecutionService executionService;

    /**
     * Принимает высокоуровневую цель, планирует и асинхронно выполняет ее.
     *
     * @param goal           Задача на естественном языке от L5-агента.
     * @param initialContext Начальный контекст с данными для выполнения.
     */
    public void executeGoal(String goal, AgentContext initialContext) {
        log.info("Автономный исполнитель получил новую цель: '{}'", goal);
        plannerAgent.createWorkflow(goal, initialContext.payload())
                .flatMap(workflow -> executionService.executeWorkflow(workflow, initialContext))
                .subscribe(
                        results -> log.info("Автономная цель '{}' успешно выполнена. Результатов: {}", goal, results.size()),
                        error -> log.error("Ошибка при выполнении автономной цели '{}'", goal, error)
                );
    }
}
