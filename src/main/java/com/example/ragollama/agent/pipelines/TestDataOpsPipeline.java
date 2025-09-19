package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.TestDataOpsOrchestratorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для мета-агента, управляющего операциями с тестовыми данными.
 */
@Component
@RequiredArgsConstructor
public class TestDataOpsPipeline implements AgentPipeline {

    private final TestDataOpsOrchestratorAgent orchestratorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-data-ops-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом-оркестратором.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(orchestratorAgent)
        );
    }
}