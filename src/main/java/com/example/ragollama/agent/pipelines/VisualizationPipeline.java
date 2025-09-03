package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.ChartGeneratorAgent;
import com.example.ragollama.optimization.DataSummarizerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VisualizationPipeline implements AgentPipeline {
    private final DataSummarizerAgent dataSummarizerAgent;
    private final ChartGeneratorAgent chartGeneratorAgent;

    @Override
    public String getName() {
        return "visualization-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два последовательных этапа: сначала агрегация данных,
     * затем генерация визуализации на их основе.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(dataSummarizerAgent),
                List.of(chartGeneratorAgent)
        );
    }
}
