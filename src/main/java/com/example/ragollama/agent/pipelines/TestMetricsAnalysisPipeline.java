package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.TestMetricsAnalyzerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Реализация конвейера для анализа исторических метрик тестов.
 * <p>
 * Этот конвейер является "фасадом" для бизнес-возможности анализа трендов
 * и состоит из одного шага, который делегирует всю работу агенту-аналитику.
 */
@Component
@RequiredArgsConstructor
public class TestMetricsAnalysisPipeline implements AgentPipeline {

    private final TestMetricsAnalyzerAgent testMetricsAnalyzer;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-metrics-analysis-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(testMetricsAnalyzer)
        );
    }
}