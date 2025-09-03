package com.example.ragollama.agent;

import com.example.ragollama.agent.analytics.domain.BugPatternDetectorAgent;
import com.example.ragollama.agent.strategy.domain.SprintPlannerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для мета-агента "AI Product Manager", формирующего план на спринт.
 */
@Component
@RequiredArgsConstructor
class SprintPlanningPipeline implements AgentPipeline {
    private final BugPatternDetectorAgent bugPatternDetector;
    private final SprintPlannerAgent sprintPlanner;

    @Override
    public String getName() {
        return "sprint-planning-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два последовательных этапа: сначала анализ паттернов багов,
     * затем планирование спринта на основе этого анализа.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(bugPatternDetector),
                List.of(sprintPlanner)
        );
    }
}
