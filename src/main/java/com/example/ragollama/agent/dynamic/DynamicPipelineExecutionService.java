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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Stateful-сервис для выполнения динамических планов, полностью переведенный на Project Reactor.
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
     * Запускает выполнение плана.
     *
     * @param plan           План для выполнения.
     * @param initialContext Начальный контекст.
     * @param sessionId      ID сессии.
     * @return {@link Mono} с финальным списком результатов.
     */
    public Mono<List<AgentResult>> executePlan(List<PlanStep> plan, AgentContext initialContext, UUID sessionId) {
        if (plan == null || plan.isEmpty()) {
            return Mono.just(List.of());
        }

        ExecutionState state = ExecutionState.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .status(ExecutionState.Status.RUNNING)
                .planSteps(plan)
                .accumulatedContext(new HashMap<>(initialContext.payload()))
                .currentStepIndex(0)
                .build();

        return executionStateRepository.save(state)
                .doOnSuccess(s -> log.info("Начало выполнения плана для executionId: {}", s.getId()))
                .flatMap(s -> executeSequentially(s, new ArrayList<>(), 0));
    }

    /**
     * Возобновляет выполнение плана после утверждения.
     *
     * @param executionId ID выполнения.
     * @return {@link Mono<Void>} сигнализирующий о завершении.
     */
    @Transactional
    public Mono<Void> resumeExecution(UUID executionId) {
        log.info("Возобновление выполнения для executionId: {}", executionId);
        return executionStateRepository.findById(executionId)
                .flatMap(state -> {
                    if (state.getStatus() == ExecutionState.Status.PENDING_APPROVAL) {
                        state.setStatus(ExecutionState.Status.RUNNING);
                        state.setResumedAfterApproval(true);
                        return executionStateRepository.save(state)
                                .flatMap(savedState ->
                                        executeSequentially(savedState, new ArrayList<>(savedState.getExecutionHistory()), 0)
                                                .doOnSuccess(results -> log.info("Возобновленный конвейер {} успешно завершен.", executionId))
                                                .doOnError(error -> log.error("Ошибка при возобновлении конвейера {}", executionId, error))
                                                .then()
                                );
                    } else {
                        log.warn("Попытка возобновить конвейер {}, который не в статусе PENDING_APPROVAL.", executionId);
                        return Mono.empty();
                    }
                });
    }

    private Mono<List<AgentResult>> executeSequentially(ExecutionState state, List<AgentResult> accumulatedResults, int recoveryAttempt) {
        if (state.getCurrentStepIndex() >= state.getPlanSteps().size()) {
            state.setStatus(ExecutionState.Status.COMPLETED);
            return executionStateRepository.save(state)
                    .doOnSuccess(s -> log.info("План для executionId: {} успешно завершен.", s.getId()))
                    .thenReturn(accumulatedResults);
        }

        PlanStep currentStep = state.getPlanSteps().get(state.getCurrentStepIndex());
        ToolRegistryService toolRegistry = toolRegistryProvider.getObject();
        QaAgent agent = toolRegistry.getAgent(currentStep.agentName())
                .orElseThrow(() -> new IllegalArgumentException("Агент '" + currentStep.agentName() + "' не найден."));

        if (agent.requiresApproval() && !state.isResumedAfterApproval()) {
            log.info("Шаг {} ('{}') требует утверждения. Приостановка выполнения.", state.getCurrentStepIndex(), agent.getName());
            state.setStatus(ExecutionState.Status.PENDING_APPROVAL);
            return executionStateRepository.save(state).map(savedState -> {
                AgentResult approvalResult = new AgentResult(
                        "human-in-the-loop-gate",
                        AgentResult.Status.SUCCESS,
                        "Требуется утверждение для шага: " + agent.getName(),
                        Map.of("executionId", savedState.getId())
                );
                accumulatedResults.add(approvalResult);
                return accumulatedResults;
            });
        }

        return executeStep(currentStep, new AgentContext(state.getAccumulatedContext()), toolRegistry)
                .flatMap(result -> {
                    accumulatedResults.add(result);
                    state.getAccumulatedContext().putAll(result.details());
                    state.getExecutionHistory().add(result);
                    state.setCurrentStepIndex(state.getCurrentStepIndex() + 1);
                    state.setResumedAfterApproval(false);
                    return executionStateRepository.save(state)
                            .flatMap(savedState -> executeSequentially(savedState, accumulatedResults, 0));
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

        return errorHandlerAgent.execute(errorContext)
                .flatMap(result -> {
                    RemediationPlan plan = (RemediationPlan) result.details().get("remediationPlan");
                    if (plan.action() == RemediationPlan.ActionType.RETRY_WITH_FIX) {
                        log.info("План исправления: RETRY_WITH_FIX. Обновление аргументов.");
                        PlanStep fixedStep = new PlanStep(failedStep.agentName(), plan.modifiedArguments());
                        currentState.getPlanSteps().set(currentState.getCurrentStepIndex(), fixedStep);
                        currentState.setResumedAfterApproval(true);
                        return executionStateRepository.save(currentState);
                    } else {
                        log.error("План исправления: FAIL_GRACEFULLY. Завершение. Причина: {}", plan.justification());
                        currentState.setStatus(ExecutionState.Status.FAILED);
                        return executionStateRepository.save(currentState)
                                .then(Mono.error(new RuntimeException("Выполнение остановлено: " + plan.justification(), error)));
                    }
                });
    }

    private Mono<AgentResult> executeStep(PlanStep step, AgentContext currentContext, ToolRegistryService toolRegistry) {
        Map<String, Object> stepPayload = new HashMap<>(currentContext.payload());
        stepPayload.putAll(step.arguments());
        AgentContext stepContext = new AgentContext(stepPayload);

        log.debug("Выполнение шага: агент '{}' с контекстом: {}", step.agentName(), stepPayload);
        QaAgent agent = toolRegistry.getAgent(step.agentName())
                .orElseThrow(() -> new IllegalArgumentException("Агент '" + step.agentName() + "' не найден."));

        return agent.execute(stepContext);
    }
}
