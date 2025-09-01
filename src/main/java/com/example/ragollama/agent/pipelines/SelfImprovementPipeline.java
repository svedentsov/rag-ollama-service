package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.InteractionAnalyzerAgent;
import com.example.ragollama.optimization.PromptRefinementAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SelfImprovementPipeline implements AgentPipeline {

    private final InteractionAnalyzerAgent analyzerAgent;
    private final PromptRefinementAgent refinementAgent;

    @Override
    public String getName() {
        return "self-improvement-pipeline";
    }

    @Override
    public List<QaAgent> getAgents() {
        return List.of(analyzerAgent, refinementAgent);
    }
}
