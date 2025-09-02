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

/**
 * Агент-аналитик, который "изучает логи" выполнения динамических планов.
 * <p>
 * Его задача — извлечь из истории выполнений статистику о сбоях и
 * часто используемых последовательностях агентов, чтобы предоставить
 * "пищу для размышлений" для агента по улучшению промптов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionAnalyzerAgent implements ToolAgent {

    private final ExecutionStateRepository executionStateRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "interaction-analyzer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует логи выполнения динамических планов и находит паттерны неэффективности или частых сбоев.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return true; // Не требует специфического входа
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            // В реальной системе здесь была бы фильтрация по дате из контекста
            List<ExecutionState> recentExecutions = executionStateRepository.findAll();

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

    /**
     * Находит все смежные пары агентов в плане.
     *
     * @param steps План выполнения.
     * @return Список строк, представляющих последовательности "Агент1 -> Агент2".
     */
    private List<String> findPairs(List<PlanStep> steps) {
        if (steps == null || steps.size() < 2) {
            return List.of();
        }
        return java.util.stream.IntStream.range(0, steps.size() - 1)
                .mapToObj(i -> steps.get(i).agentName() + " -> " + steps.get(i + 1).agentName())
                .toList();
    }
}
