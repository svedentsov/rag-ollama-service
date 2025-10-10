package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.openapi.domain.SpecDriftSentinelAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для обнаружения расхождений между OpenAPI спецификацией и кодом.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link SpecDriftSentinelAgent}.
 */
@Component
@RequiredArgsConstructor
public class SpecDriftSentinelPipeline implements AgentPipeline {

    private final SpecDriftSentinelAgent sentinelAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "spec-drift-sentinel-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(List.of(sentinelAgent));
    }
}
