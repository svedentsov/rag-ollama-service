package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.dynamic.ExecutionState;
import com.example.ragollama.agent.dynamic.ExecutionStateRepository;
import com.example.ragollama.agent.dynamic.PlanStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionAnalyzerAgent implements ToolAgent {

    private final ExecutionStateRepository executionStateRepository;

    @Override
    public String getName() { return "interaction-analyzer"; }
    @Override
    public String getDescription() { return "Анализирует логи выполнения и находит паттерны неэффективности."; }
    @Override
    public boolean canHandle(AgentContext context) { return true; }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            List<ExecutionState> recentExecutions = executionStateRepository.findAll(); // В реальной системе нужна фильтрация по дате

            // Находим самые частые последовательности из 2-х агентов
            Map<String, Long> frequentPairs = recentExecutions.stream()
                    .flatMap(exec -> findPairs(exec.getPlanSteps()).stream())
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            // Находим агентов, которые часто падают
            Map<String, Long> frequentFailures = recentExecutions.stream()
                    .filter(exec -> exec.getStatus() == ExecutionState.Status.FAILED)
                    .map(exec -> exec.getPlanSteps().get(exec.getCurrentStepIndex()).agentName())
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            Map<String, Object> analysis = Map.of(
                    "frequentPairs", frequentPairs,
                    "frequentFailures", frequentFailures
            );

            return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Анализ взаимодействий завершен.", Map.of("interactionAnalysis", analysis));
        });
    }

    private List<String> findPairs(List<PlanStep> steps) {
        if (steps == null || steps.size() < 2) {
            return List.of();
        }
        return java.util.stream.IntStream.range(0, steps.size() - 1)
                .mapToObj(i -> steps.get(i).agentName() + " -> " + steps.get(i+1).agentName())
                .toList();
    }
}
