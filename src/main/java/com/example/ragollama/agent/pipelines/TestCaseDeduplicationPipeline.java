package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.TestCaseDeduplicatorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для поиска дубликатов тест-кейсов.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link TestCaseDeduplicatorAgent}.
 */
@Component
@RequiredArgsConstructor
public class TestCaseDeduplicationPipeline implements AgentPipeline {
    private final TestCaseDeduplicatorAgent deduplicatorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-case-deduplication-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(deduplicatorAgent)
        );
    }
}
