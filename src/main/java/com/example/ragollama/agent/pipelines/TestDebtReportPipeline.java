package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.TestDebtAnalyzerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер, реализующий полный цикл анализа тестового технического долга.
 * <p>
 * В данной реализации состоит из одного агента, но может быть расширен
 * дополнительными шагами в будущем (например, для обогащения данных).
 */
@Component
@RequiredArgsConstructor
public class TestDebtReportPipeline implements AgentPipeline {

    private final TestDebtAnalyzerAgent testDebtAnalyzerAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-debt-report-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QaAgent> getAgents() {
        return List.of(testDebtAnalyzerAgent);
    }
}
