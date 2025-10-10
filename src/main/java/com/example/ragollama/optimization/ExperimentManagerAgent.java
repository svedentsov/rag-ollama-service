package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.evaluation.model.EvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * AI-агент, который оркестрирует запуск A/B-тестов для различных
 * конфигураций RAG-конвейера, собирая результаты для последующего анализа.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExperimentManagerAgent implements ToolAgent {

    private final IsolatedEvaluationRunner evaluationRunner;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "experiment-manager-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Оркестрирует запуск A/B-тестов для различных RAG-конфигураций.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("variants");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Асинхронно запускает оценку для базовой конфигурации и для каждого
     * варианта, а затем собирает все результаты в единую карту для
     * передачи следующему агенту в конвейере.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        Map<String, Map<String, Object>> variants = (Map<String, Map<String, Object>>) context.payload().get("variants");

        Mono<Map.Entry<String, EvaluationResult>> baselineMono = evaluationRunner.runEvaluationWithOverrides(Map.of())
                .map(result -> Map.entry("baseline", result));

        Flux<Map.Entry<String, EvaluationResult>> variantsFlux = Flux.fromIterable(variants.entrySet())
                .flatMap(variantEntry -> evaluationRunner.runEvaluationWithOverrides(variantEntry.getValue())
                        .map(result -> Map.entry(variantEntry.getKey(), result)));

        return Flux.concat(baselineMono, variantsFlux)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(resultsMap -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Все варианты эксперимента успешно оценены.",
                        Map.of("experimentResults", new HashMap<>(resultsMap))
                ));
    }
}
