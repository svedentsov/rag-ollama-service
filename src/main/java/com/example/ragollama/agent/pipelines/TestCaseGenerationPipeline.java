package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.TestCaseGeneratorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для генерации структурированных тест-кейсов
 * из текстового описания требований.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link TestCaseGeneratorAgent}.
 */
@Component
@RequiredArgsConstructor
public class TestCaseGenerationPipeline implements AgentPipeline {

    private final TestCaseGeneratorAgent testCaseGenerator;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-case-generation-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(testCaseGenerator)
        );
    }
}
