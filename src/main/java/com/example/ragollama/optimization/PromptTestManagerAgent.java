package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.evaluation.model.EvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
public class PromptTestManagerAgent implements ToolAgent {

    private final IsolatedEvaluationRunner evaluationRunner;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "prompt-test-manager-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Оркестрирует A/B-тест для новой версии промпта.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("promptName") && context.payload().containsKey("newPromptContent");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String promptName = (String) context.payload().get("promptName");
        String newPromptContent = (String) context.payload().get("newPromptContent");
        log.info("Запуск A/B-теста для промпта '{}'", promptName);

        Mono<EvaluationResult> baselineMono = evaluationRunner.runWithPromptOverride(promptName, null);
        Mono<EvaluationResult> variantMono = evaluationRunner.runWithPromptOverride(promptName, newPromptContent);

        return Mono.zip(baselineMono, variantMono)
                .map(tuple -> {
                    Map<String, Object> results = new HashMap<>();
                    results.put("baseline", tuple.getT1());
                    results.put("variant", tuple.getT2());

                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "A/B-тестирование промпта завершено.",
                            Map.of("experimentResults", results)
                    );
                });
    }
}
