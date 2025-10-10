package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.TestVerifierAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для проверки качества кода автотеста.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link TestVerifierAgent}.
 */
@Component
@RequiredArgsConstructor
public class TestVerifierPipeline implements AgentPipeline {

    private final TestVerifierAgent verifierAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-verifier-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(List.of(verifierAgent));
    }
}
