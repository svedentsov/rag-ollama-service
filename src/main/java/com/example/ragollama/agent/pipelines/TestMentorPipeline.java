package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.TestMentorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для агента-наставника по тестированию.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link TestMentorAgent} для проведения глубокого код-ревью автотеста.
 */
@Component
@RequiredArgsConstructor
public class TestMentorPipeline implements AgentPipeline {

    private final TestMentorAgent mentorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-mentor-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(List.of(mentorAgent));
    }
}
