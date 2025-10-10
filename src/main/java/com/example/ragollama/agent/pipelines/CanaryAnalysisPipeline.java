package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.analytics.domain.CanaryAnalyzerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для анализа метрик Canary-развертывания.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link CanaryAnalyzerAgent}. Этот агент выполняет статистический анализ
 * и использует LLM для интерпретации результатов.
 */
@Component
@RequiredArgsConstructor
public class CanaryAnalysisPipeline implements AgentPipeline {

    private final CanaryAnalyzerAgent canaryAnalyzer;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "canary-analysis-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(canaryAnalyzer)
        );
    }
}
