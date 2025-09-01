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

    @Override
    public List<QaAgent> getAgents() {
        return List.of(dataSummarizerAgent, chartGeneratorAgent);
    }
}
