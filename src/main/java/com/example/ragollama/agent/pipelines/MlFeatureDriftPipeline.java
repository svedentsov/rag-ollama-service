package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.mlops.domain.MlDriftGuardAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для обнаружения дрейфа признаков в ML-моделях.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link MlDriftGuardAgent}.
 */
@Component
@RequiredArgsConstructor
public class MlFeatureDriftPipeline implements AgentPipeline {

    private final MlDriftGuardAgent driftGuardAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "ml-feature-drift-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(List.of(driftGuardAgent));
    }
}
