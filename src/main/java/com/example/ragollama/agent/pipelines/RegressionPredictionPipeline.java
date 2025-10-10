package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.analytics.domain.RegressionPredictorAgent;
import com.example.ragollama.agent.coverage.domain.CoverageAuditorAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для прогнозирования регрессионных рисков.
 * <p>
 * Этот конвейер является примером сложной аналитической цепочки, где
 * данные последовательно собираются и обогащаются на каждом шаге:
 * <ol>
 *     <li>{@link GitInspectorAgent} находит измененные файлы.</li>
 *     <li>{@link CoverageAuditorAgent} анализирует их тестовое покрытие.</li>
 *     <li>{@link RegressionPredictorAgent} синтезирует данные о покрытии
 *     и историю дефектов для финального прогноза.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class RegressionPredictionPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final CoverageAuditorAgent coverageAuditor;
    private final RegressionPredictorAgent regressionPredictor;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "regression-prediction-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список из трех последовательных этапов.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspector),
                List.of(coverageAuditor),
                List.of(regressionPredictor)
        );
    }
}
