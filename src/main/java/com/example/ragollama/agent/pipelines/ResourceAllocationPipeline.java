package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.MetricsFetcherAgent;
import com.example.ragollama.optimization.ResourceAllocatorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ResourceAllocationPipeline implements AgentPipeline {

    private final MetricsFetcherAgent metricsFetcherAgent;
    private final ResourceAllocatorAgent resourceAllocatorAgent;

    @Override
    public String getName() {
        return "resource-allocation-pipeline";
    }

    @Override
    public List<QaAgent> getAgents() {
        return List.of(metricsFetcherAgent, resourceAllocatorAgent);
    }
}
