package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.ConsistencyCheckerAgent;
import com.example.ragollama.optimization.CrossValidatorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер, реализующий полный цикл проверки консистентности утверждения.
 * <p>
 * Этот конвейер является примером композиции двух атомарных агентов:
 * 1. {@link ConsistencyCheckerAgent} собирает "доказательства".
 * 2. {@link CrossValidatorAgent} анализирует их и выносит вердикт.
 */
@Component
@RequiredArgsConstructor
public class ConsistencyCheckPipeline implements AgentPipeline {

    private final ConsistencyCheckerAgent consistencyCheckerAgent;
    private final CrossValidatorAgent crossValidatorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "consistency-check-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QaAgent> getAgents() {
        return List.of(consistencyCheckerAgent, crossValidatorAgent);
    }
}
