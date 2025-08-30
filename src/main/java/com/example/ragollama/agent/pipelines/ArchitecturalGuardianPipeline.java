package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.architecture.domain.ArchConsistencyMapperAgent;
import com.example.ragollama.agent.architecture.domain.ArchitectureReviewSynthesizerAgent;
import com.example.ragollama.agent.compliance.domain.PrivacyComplianceAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import com.example.ragollama.agent.performance.domain.PerformanceBottleneckFinderAgent;
import com.example.ragollama.agent.testanalysis.domain.TestMentorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для мета-агента "AI Architecture Governor".
 */
@Component
@RequiredArgsConstructor
class ArchitecturalGuardianPipeline implements AgentPipeline {
    private final GitInspectorAgent gitInspector;
    private final ArchConsistencyMapperAgent archConsistencyMapper;
    private final TestMentorAgent testMentorBot;
    private final PerformanceBottleneckFinderAgent performanceBottleneckFinder;
    private final PrivacyComplianceAgent privacyComplianceChecker;
    private final ArchitectureReviewSynthesizerAgent reviewSynthesizer;

    @Override
    public String getName() {
        return "architectural-guardian-pipeline";
    }

    @Override
    public List<QaAgent> getAgents() {
        return List.of(gitInspector, archConsistencyMapper, testMentorBot,
                performanceBottleneckFinder, privacyComplianceChecker, reviewSynthesizer);
    }
}
