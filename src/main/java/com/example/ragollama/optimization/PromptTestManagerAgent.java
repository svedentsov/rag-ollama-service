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
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который оркестрирует A/B-тестирование для одной новой версии промпта.
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
     * <p>
     * Асинхронно и параллельно запускает два прогона оценки: один для
     * базовой версии промпта, другой для новой.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
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
                })
                .toFuture();
    }
}