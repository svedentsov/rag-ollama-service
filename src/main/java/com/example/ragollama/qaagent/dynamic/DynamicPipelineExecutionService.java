package com.example.ragollama.qaagent.dynamic;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Stateful-сервис, отвечающий за выполнение, приостановку и возобновление
 * планов, сгенерированных {@link PlanningAgentService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPipelineExecutionService {

    private final ToolRegistryService toolRegistry;
    private final ExecutionStateRepository executionStateRepository;

    public Mono<List<AgentResult>> executePlan(List<PlanStep> plan, AgentContext initialContext, UUID sessionId) {
        if (plan == null || plan.isEmpty()) {
            return Mono.just(List.of());
        }

        ExecutionState state = ExecutionState.builder()
                .sessionId(sessionId)
                .status(ExecutionState.Status.RUNNING)
                .planSteps(plan)
                .accumulatedContext(initialContext.payload())
                .currentStepIndex(0)
                .build();
        executionStateRepository.save(state);

        log.info("Начало выполнения плана для executionId: {}", state.getId());
        return runPipelineFrom(state);
    }

    @Async("applicationTaskExecutor")
    @Transactional
    public void resumeExecution(UUID executionId) {
        log.info("Возобновление выполнения для executionId: {}", executionId);
        executionStateRepository.findById(executionId).ifPresent(state -> {
            if (state.getStatus() == ExecutionState.Status.PENDING_APPROVAL) {
                state.setStatus(ExecutionState.Status.RUNNING);
                executionStateRepository.save(state);
                runPipelineFrom(state).subscribe(
                        results -> log.info("Возобновленный конвейер {} успешно завершен.", executionId),
                        error -> log.error("Ошибка при возобновлении конвейера {}", executionId, error)
                );
            } else {
                log.warn("Попытка возобновить конвейер {}, который не находится в статусе PENDING_APPROVAL.", executionId);
            }
        });
    }

    private Mono<List<AgentResult>> runPipelineFrom(ExecutionState state) {
        List<AgentResult> accumulatedResults = new ArrayList<>();
        Mono<ExecutionState> executionChain = Mono.just(state);

        for (int i = state.getCurrentStepIndex(); i < state.getPlanSteps().size(); i++) {
            final int currentStepIndex = i;
            PlanStep step = state.getPlanSteps().get(currentStepIndex);
            QaAgent agent = toolRegistry.getAgent(step.agentName())
                    .orElseThrow(() -> new IllegalArgumentException("Агент '" + step.agentName() + "' не найден."));

            if (agent.requiresApproval() && state.getStatus() != ExecutionState.Status.RESUMED_AFTER_APPROVAL) {
                log.info("Шаг {} ('{}') требует утверждения. Приостановка выполнения.", currentStepIndex, agent.getName());
                state.setCurrentStepIndex(currentStepIndex);
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

            executionChain = executionChain.flatMap(currentState ->
                    executeStep(step, new AgentContext(currentState.getAccumulatedContext()))
                            .doOnNext(result -> {
                                accumulatedResults.add(result);
                                currentState.getAccumulatedContext().putAll(result.details());
                                if (currentState.getStatus() == ExecutionState.Status.RESUMED_AFTER_APPROVAL) {
                                    currentState.setStatus(ExecutionState.Status.RUNNING);
                                }
                            })
                            .thenReturn(currentState)
            );
        }

        return executionChain.map(finalState -> {
            finalState.setStatus(ExecutionState.Status.COMPLETED);
            executionStateRepository.save(finalState);
            log.info("План для executionId: {} успешно завершен.", finalState.getId());
            return accumulatedResults;
        });
    }

    private Mono<AgentResult> executeStep(PlanStep step, AgentContext currentContext) {
        Map<String, Object> stepPayload = new HashMap<>(currentContext.payload());
        stepPayload.putAll(step.arguments());
        AgentContext stepContext = new AgentContext(stepPayload);

        log.debug("Выполнение шага: агент '{}' с контекстом: {}", step.agentName(), stepPayload);
        QaAgent agent = toolRegistry.getAgent(step.agentName()).orElseThrow();

        return Mono.fromFuture(() -> agent.execute(stepContext));
    }
}
