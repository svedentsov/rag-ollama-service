// Упрощенный PrioritizationPipeline
package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.PrioritizationAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Конвейер, реализующий полный цикл автоматической приоритизации задач.
 * <p>
 * В этой версии конвейер стал атомарным и состоит из одного мета-агента,
 * который инкапсулирует всю сложную логику, включая сбор данных.
 */
@Component
@RequiredArgsConstructor
public class PrioritizationPipeline implements AgentPipeline {

    private final PrioritizationAgent prioritizationAgent;

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "prioritization-pipeline";
    }

    /** {@inheritDoc} */
    @Override
    public List<QaAgent> getAgents() {
        return List.of(prioritizationAgent);
    }
}
