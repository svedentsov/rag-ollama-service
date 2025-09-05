package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import com.example.ragollama.agent.performance.domain.PerformancePredictorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для прогнозирования влияния изменений на производительность.
 */
@Component
@RequiredArgsConstructor
public class PerformancePredictionPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final PerformancePredictorAgent performancePredictor;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "performance-prediction-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Определяет два последовательных этапа:
     * <ol>
     *   <li>Сбор измененных файлов.</li>
     *   <li>Анализ этих файлов и генерация прогноза.</li>
     * </ol>
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspector),
                List.of(performancePredictor)
        );
    }
}
