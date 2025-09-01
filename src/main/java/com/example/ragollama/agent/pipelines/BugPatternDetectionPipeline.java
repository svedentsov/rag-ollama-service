package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.analytics.domain.BugPatternDetectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер, реализующий полный цикл обнаружения паттернов багов
 * в исторических данных.
 */
@Component
@RequiredArgsConstructor
public class BugPatternDetectionPipeline implements AgentPipeline {

    private final BugPatternDetectorAgent bugPatternDetectorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "bug-pattern-detection-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QaAgent> getAgents() {
        return List.of(bugPatternDetectorAgent);
    }
}
