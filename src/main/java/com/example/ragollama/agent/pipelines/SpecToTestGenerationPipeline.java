package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.openapi.domain.SpecToTestGeneratorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для генерации API-теста из OpenAPI спецификации.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link SpecToTestGeneratorAgent}.
 */
@Component
@RequiredArgsConstructor
public class SpecToTestGenerationPipeline implements AgentPipeline {

    private final SpecToTestGeneratorAgent testGenerator;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "spec-to-test-generation-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(List.of(testGenerator));
    }
}
