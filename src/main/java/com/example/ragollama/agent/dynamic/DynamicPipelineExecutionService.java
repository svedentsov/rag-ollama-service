package com.example.ragollama.agent.dynamic;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.registry.ToolRegistryService;
import com.example.ragollama.optimization.ErrorHandlerAgent;
import com.example.ragollama.optimization.model.RemediationPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Stateful-сервис, отвечающий за выполнение, приостановку, возобновление
 * и **самовосстановление** планов, сгенерированных `PlanningAgentService`.
 * <p>
 * Эта версия использует рекурсивный подход для выполнения плана, что позволяет
 * элегантно реализовать логику самовосстановления при сбоях.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPipelineExecutionService {

    private final ObjectProvider<ToolRegistryService> toolRegistryProvider;
    private final ExecutionStateRepository executionStateRepository;
    private final ErrorHandlerAgent errorHandlerAgent;
    private static final int MAX_RECOVERY_ATTEMPTS = 2;

    /**
     * Запускает выполнение нового плана.
     *
     * @param plan           Список шагов для выполнения.
     * @param initialContext Начальный контекст.
     * @param sessionId      ID сессии для трассировки.
     * @return {@link Mono} с финальным списком результатов.
     */
    public Mono<List<AgentResult>> executePlan(List<PlanStep> plan, AgentContext initialContext, UUID sessionId) {
        if (plan == null || plan.isEmpty()) {
            return Mono.just(List.of());
        }

        ExecutionState state = ExecutionState.builder()
                .sessionId(sessionId)
                .status(ExecutionState.Status.RUNNING)
                .planSteps(plan)
                .accumulatedContext(new HashMap<>(initialContext.payload())) // Создаем изменяемую копию
                .currentStepIndex(0)
                .build();
        executionStateRepository.save(state);

        log.info("Начало выполнения плана для executionId: {}", state.getId());
        return executeSequentially(state, new ArrayList<>(), 0);
    }

    /**
     * Асинхронно возобновляет выполнение плана, ожидающего утверждения.
     *
     * @param executionId ID выполнения.
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public void resumeExecution(UUID executionId) {
        log.info("Возобновление выполнения для executionId: {}", executionId);
        executionStateRepository.findById(executionId).ifPresent(state -> {
            if (state.getStatus() == ExecutionState.Status.PENDING_APPROVAL) {
                state.setStatus(ExecutionState.Status.RUNNING);
                state.setResumedAfterApproval(true);
                executionStateRepository.save(state);
                executeSequentially(state, new ArrayList<>(state.getExecutionHistory()), 0)
                        .subscribe(
                                results -> log.info("Возобновленный конвейер {} успешно завершен.", executionId),
                                error -> log.error("Ошибка при возобновлении конвейера {}", executionId, error)
                        );
            } else {
                log.warn("Попытка возобновить конвейер {}, который не находится в статусе PENDING_APPROVAL.", executionId);
            }
        });
    }

    private Mono<List<AgentResult>> executeSequentially(ExecutionState state, List<AgentResult> accumulatedResults, int recoveryAttempt) {
        if (state.getCurrentStepIndex() >= state.getPlanSteps().size()) {
            state.setStatus(ExecutionState.Status.COMPLETED);
            executionStateRepository.save(state);
            log.info("План для executionId: {} успешно завершен.", state.getId());
            return Mono.just(accumulatedResults);
        }

        PlanStep currentStep = state.getPlanSteps().get(state.getCurrentStepIndex());
        ToolRegistryService toolRegistry = toolRegistryProvider.getObject();
        QaAgent agent = toolRegistry.getAgent(currentStep.agentName())
                .orElseThrow(() -> new IllegalArgumentException("Агент '" + currentStep.agentName() + "' не найден."));

        if (agent.requiresApproval() && !state.isResumedAfterApproval()) {
            log.info("Шаг {} ('{}') требует утверждения. Приостановка выполнения.", state.getCurrentStepIndex(), agent.getName());
            state.setStatus(ExecutionState.Status.PENDING_APPROVAL);
            executionStateRepository.save(state);
            AgentResult approvalResult = new AgentResult(
                    "human-in-the-loop-gate",
                    AgentResult.Status.SUCCESS,
                    "Требуется утверждение для шага: " + agent.getName(),
                    Map.of("executionId", state.getId())
            );
            accumulatedResults.add(approvalResult);
            return Mono.just(accumulatedResults);
        }

        return executeStep(currentStep, new AgentContext(state.getAccumulatedContext()), toolRegistry)
                .flatMap(result -> {
                    accumulatedResults.add(result);
                    state.getAccumulatedContext().putAll(result.details());
                    state.getExecutionHistory().add(result);
                    state.setCurrentStepIndex(state.getCurrentStepIndex() + 1);
                    state.setResumedAfterApproval(false);
                    executionStateRepository.save(state);
                    return executeSequentially(state, accumulatedResults, 0);
                })
                .onErrorResume(error -> {
                    if (recoveryAttempt >= MAX_RECOVERY_ATTEMPTS) {
                        return Mono.error(new RuntimeException("Превышен лимит попыток самовосстановления.", error));
                    }
                    log.warn("Ошибка на шаге {}: '{}'. Запуск ErrorHandlerAgent...", state.getCurrentStepIndex(), error.getMessage());
                    return handleStepError(error, currentStep, state)
                            .flatMap(newState -> executeSequentially(newState, accumulatedResults, recoveryAttempt + 1));
                });
    }

    private Mono<ExecutionState> handleStepError(Throwable error, PlanStep failedStep, ExecutionState currentState) {
        AgentContext errorContext = new AgentContext(Map.of(
                "failedAgentName", failedStep.agentName(),
                "inputArguments", failedStep.arguments(),
                "errorMessage", error.getMessage(),
                "stackTrace", ExceptionUtils.getStackTrace(error)
        ));

        return Mono.fromFuture(() -> errorHandlerAgent.execute(errorContext))
                .flatMap(result -> {
                    RemediationPlan plan = (RemediationPlan) result.details().get("remediationPlan");
                    if (plan.action() == RemediationPlan.ActionType.RETRY_WITH_FIX) {
                        log.info("План исправления: RETRY_WITH_FIX. Обновление аргументов.");
                        PlanStep fixedStep = new PlanStep(failedStep.agentName(), plan.modifiedArguments());
                        currentState.getPlanSteps().set(currentState.getCurrentStepIndex(), fixedStep);
                        currentState.setResumedAfterApproval(true);
                        executionStateRepository.save(currentState);
                        return Mono.just(currentState);
                    } else {
                        log.error("План исправления: FAIL_GRACEFULLY. Завершение конвейера. Причина: {}", plan.justification());
                        currentState.setStatus(ExecutionState.Status.FAILED);
                        executionStateRepository.save(currentState);
                        return Mono.error(new RuntimeException("Выполнение остановлено по плану исправления: " + plan.justification(), error));
                    }
                });
    }

    private Mono<AgentResult> executeStep(PlanStep step, AgentContext currentContext, ToolRegistryService toolRegistry) {
        Map<String, Object> stepPayload = new HashMap<>(currentContext.payload());
        stepPayload.putAll(step.arguments());
        AgentContext stepContext = new AgentContext(stepPayload);

        log.debug("Выполнение шага: агент '{}' с контекстом: {}", step.agentName(), stepPayload);
        QaAgent agent = toolRegistry.getAgent(step.agentName())
                .orElseThrow(() -> new IllegalArgumentException("Агент '" + step.agentName() + "' не найден в реестре."));

        return Mono.fromFuture(() -> agent.execute(stepContext));
    }
}