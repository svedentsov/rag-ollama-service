package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.ExperimentAnalysisAgent;
import com.example.ragollama.optimization.PromptTestManagerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для проведения A/B-тестирования промптов.
 */
@Component
@RequiredArgsConstructor
public class PromptTestingPipeline implements AgentPipeline {

    private final PromptTestManagerAgent testManagerAgent;
    private final ExperimentAnalysisAgent analysisAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "prompt-testing-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два последовательных этапа:
     * <ol>
     *     <li>Запуск A/B-теста.</li>
     *     <li>Анализ результатов и объявление победителя.</li>
     * </ol>
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(testManagerAgent),
                List.of(analysisAgent)
        );
    }
}