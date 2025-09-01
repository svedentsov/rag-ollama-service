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

    @Override
    public List<QaAgent> getAgents() {
        return List.of(managerAgent, analysisAgent);
    }
}
