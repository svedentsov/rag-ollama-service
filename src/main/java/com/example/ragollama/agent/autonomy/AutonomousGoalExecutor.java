package com.example.ragollama.agent.autonomy;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.optimization.WorkflowExecutionService;
import com.example.ragollama.optimization.WorkflowPlannerAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Сервис-фасад, предоставляющий единую точку входа для запуска
 * автономных, высокоуровневых задач.
 * <p>
 * Эта версия полностью переведена на Project Reactor и возвращает {@link Mono},
 * позволяя вызывающей стороне асинхронно дождаться результата.
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
     * @return {@link Mono} со списком финальных результатов.
     */
    public Mono<List<AgentResult>> executeGoal(String goal, AgentContext initialContext) {
        log.info("Автономный исполнитель получил новую цель: '{}'", goal);
        return plannerAgent.createWorkflow(goal, initialContext.payload())
                .flatMap(workflow -> executionService.executeWorkflow(workflow, initialContext))
                .doOnSuccess(results -> log.info("Автономная цель '{}' успешно выполнена. Результатов: {}", goal, results.size()))
                .doOnError(error -> log.error("Ошибка при выполнении автономной цели '{}'", goal, error));
    }
}
