package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.TestPrioritizerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для обработки событий Pull Request из GitHub.
 * <p>
 * Основная задача этого конвейера — проанализировать изменения в PR
 * и приоритизировать релевантные тесты для запуска в CI/CD, экономя время и ресурсы.
 */
@Component
@RequiredArgsConstructor
public class GithubPrPipeline implements AgentPipeline {

    private final TestPrioritizerAgent prioritizerAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "github-pr-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(prioritizerAgent)
        );
    }
}
