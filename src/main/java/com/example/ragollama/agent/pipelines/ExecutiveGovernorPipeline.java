package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.strategy.domain.FederatedInsightsAgent;
import com.example.ragollama.agent.strategy.domain.StrategicInitiativePlannerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для мета-агента "Executive Portfolio Governor".
 * <p>
 * Реализует двухступенчатый процесс: сначала сбор и анализ данных по всем
 * проектам, а затем — синтез стратегического плана на основе этого анализа.
 */
@Component
@RequiredArgsConstructor
public class ExecutiveGovernorPipeline implements AgentPipeline {

    private final FederatedInsightsAgent federatedInsightsAgent;
    private final StrategicInitiativePlannerAgent strategicInitiativePlannerAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "executive-governor-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QaAgent> getAgents() {
        return List.of(
                federatedInsightsAgent,
                strategicInitiativePlannerAgent
        );
    }
}
