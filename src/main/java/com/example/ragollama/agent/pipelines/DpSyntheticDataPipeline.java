package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.datageneration.impl.DifferentiallyPrivateDataAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для генерации синтетических данных с гарантиями
 * дифференциальной приватности (DP).
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link DifferentiallyPrivateDataAgent}.
 */
@Component
@RequiredArgsConstructor
public class DpSyntheticDataPipeline implements AgentPipeline {

    private final DifferentiallyPrivateDataAgent dpAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "dp-synthetic-data-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(dpAgent)
        );
    }
}
