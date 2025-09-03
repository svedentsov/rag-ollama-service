package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.executive.domain.EngineeringVelocityGovernorAgent;
import com.example.ragollama.agent.executive.domain.fetchers.CiCdMetricsFetcherAgent;
import com.example.ragollama.agent.executive.domain.fetchers.GitMetricsFetcherAgent;
import com.example.ragollama.agent.executive.domain.fetchers.JiraMetricsFetcherAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для "Губернатора Инженерной Производительности".
 */
@Component
@RequiredArgsConstructor
public class EngineeringVelocityPipeline implements AgentPipeline {

    // Шаг 1: Сборщики метрик
    private final CiCdMetricsFetcherAgent ciCdMetricsFetcherAgent;
    private final GitMetricsFetcherAgent gitMetricsFetcherAgent;
    private final JiraMetricsFetcherAgent jiraMetricsFetcherAgent;

    // Шаг 2: Агент-синтезатор
    private final EngineeringVelocityGovernorAgent governorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "engineering-velocity-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два этапа:
     * 1. Параллельный сбор метрик из всех источников (CI/CD, Git, Jira).
     * 2. Синтез отчета на основе собранных метрик.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(ciCdMetricsFetcherAgent, gitMetricsFetcherAgent, jiraMetricsFetcherAgent),
                List.of(governorAgent)
        );
    }
}
