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

    /**
     * {@inheritDoc}
     * <p>
     * Определяет три этапа:
     * 1. Получение измененных файлов.
     * 2. Параллельный запуск всех аналитических агентов.
     * 3. Финальный синтез отчета на основе результатов всех анализаторов.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspector),
                List.of(archConsistencyMapper, testMentorBot, performanceBottleneckFinder, privacyComplianceChecker),
                List.of(reviewSynthesizer)
        );
    }
}
