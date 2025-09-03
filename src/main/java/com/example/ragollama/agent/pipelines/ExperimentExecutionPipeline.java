package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.ExperimentAnalysisAgent;
import com.example.ragollama.optimization.ExperimentManagerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExperimentExecutionPipeline implements AgentPipeline {

    private final ExperimentManagerAgent managerAgent;
    private final ExperimentAnalysisAgent analysisAgent;

    @Override
    public String getName() {
        return "experiment-execution-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два последовательных этапа: сначала запуск эксперимента,
     * затем анализ его результатов.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(managerAgent),
                List.of(analysisAgent)
        );
    }
}
