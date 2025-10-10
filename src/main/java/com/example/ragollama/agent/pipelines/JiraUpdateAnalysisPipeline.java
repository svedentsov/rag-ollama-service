package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.jira.domain.JiraFetcherAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для обработки события обновления задачи в Jira.
 * <p>
 * Конвейер запускает {@link JiraFetcherAgent} для получения актуальных
 * данных о задаче, чтобы обогатить контекст для последующих (потенциальных)
 * шагов анализа.
 */
@Component
@RequiredArgsConstructor
public class JiraUpdateAnalysisPipeline implements AgentPipeline {

    private final JiraFetcherAgent jiraFetcher;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "jira-update-analysis-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(jiraFetcher)
        );
    }
}
