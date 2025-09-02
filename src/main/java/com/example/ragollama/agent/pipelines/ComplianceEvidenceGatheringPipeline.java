package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.compliance.domain.ComplianceReportGeneratorAgent;
import com.example.ragollama.agent.compliance.domain.RequirementLinkerAgent;
import com.example.ragollama.agent.coverage.domain.TestGapAnalyzerAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import com.example.ragollama.agent.security.domain.SastAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер, реализующий полный цикл сбора доказательств и генерации
 * аудиторского отчета о соответствии (compliance).
 */
@Component
@RequiredArgsConstructor
public class ComplianceEvidenceGatheringPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final RequirementLinkerAgent requirementLinker;
    private final SastAgent sastAgent;
    private final TestGapAnalyzerAgent testGapAnalyzer;
    private final ComplianceReportGeneratorAgent reportGenerator;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "compliance-evidence-gathering-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QaAgent> getAgents() {
        return List.of(
                gitInspector,
                requirementLinker,
                sastAgent,
                testGapAnalyzer,
                reportGenerator
        );
    }
}
