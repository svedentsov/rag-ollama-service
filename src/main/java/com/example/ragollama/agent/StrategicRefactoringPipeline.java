package com.example.ragollama.agent;

import com.example.ragollama.agent.analytics.domain.BugPatternDetectorAgent;
import com.example.ragollama.agent.strategy.domain.RefactoringStrategistAgent;
import com.example.ragollama.agent.testanalysis.domain.TestDebtAnalyzerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для мета-агента "AI Tech Lead", формирующего стратегию рефакторинга.
 */
@Component
@RequiredArgsConstructor
class StrategicRefactoringPipeline implements AgentPipeline {
    private final TestDebtAnalyzerAgent testDebtAnalyzer;
    private final BugPatternDetectorAgent bugPatternDetector;
    private final RefactoringStrategistAgent refactoringStrategist;

    @Override
    public String getName() {
        return "strategic-refactoring-pipeline";
    }

    @Override
    public List<QaAgent> getAgents() {
        return List.of(testDebtAnalyzer, bugPatternDetector, refactoringStrategist);
    }
}
