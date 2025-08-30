package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация составного конвейера для воспроизведения багов.
 * <p>
 * Демонстрирует, как "Стратегия" может состоять из нескольких
 * последовательных шагов (агентов).
 */
@Component
@RequiredArgsConstructor
public class BugReproductionPipeline implements AgentPipeline {

    private final BugReportSummarizerAgent summarizerAgent;
    private final BugReproScriptGeneratorAgent scriptGeneratorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "bug-reproduction-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QaAgent> getAgents() {
        return List.of(summarizerAgent, scriptGeneratorAgent);
    }
}
