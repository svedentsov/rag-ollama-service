package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.TestSmellRefactorerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Реализация конвейера для рефакторинга автотеста.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link TestSmellRefactorerAgent}.
 */
@Component
@RequiredArgsConstructor
public class TestSmellRefactoringPipeline implements AgentPipeline {

    private final TestSmellRefactorerAgent refactorerAgent;

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "test-smell-refactoring-pipeline";
    }

    /** {@inheritDoc} */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(List.of(refactorerAgent));
    }
}
