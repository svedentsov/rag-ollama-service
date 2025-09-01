package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.CurationCandidateFinderAgent;
import com.example.ragollama.optimization.DocumentEnhancerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KnowledgeCurationPipeline implements AgentPipeline {
    private final CurationCandidateFinderAgent finderAgent;
    private final DocumentEnhancerAgent enhancerAgent;

    @Override
    public String getName() {
        return "knowledge-curation-pipeline";
    }

    @Override
    public List<QaAgent> getAgents() {
        return List.of(finderAgent, enhancerAgent);
    }
}
