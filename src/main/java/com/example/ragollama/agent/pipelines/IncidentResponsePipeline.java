package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import com.example.ragollama.agent.incidentresponse.domain.IncidentSummarizerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для мета-агента "AI On-Call Engineer".
 */
@Component
@RequiredArgsConstructor
class IncidentResponsePipeline implements AgentPipeline {
    private final GitInspectorAgent gitInspector;
    private final IncidentSummarizerAgent incidentSummarizer;

    @Override
    public String getName() {
        return "incident-response-pipeline";
    }

    @Override
    public List<QaAgent> getAgents() {
        return List.of(gitInspector, incidentSummarizer);
    }
}
