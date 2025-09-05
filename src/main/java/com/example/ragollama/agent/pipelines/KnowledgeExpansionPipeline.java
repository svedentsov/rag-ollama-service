package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.knowledgegaps.domain.KnowledgeGapClustererAgent;
import com.example.ragollama.agent.knowledgegaps.domain.KnowledgeGapFetcherAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для анализа и приоритизации пробелов в базе знаний.
 */
@Component
@RequiredArgsConstructor
public class KnowledgeExpansionPipeline implements AgentPipeline {

    private final KnowledgeGapFetcherAgent fetcherAgent;
    private final KnowledgeGapClustererAgent clustererAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "knowledge-expansion-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Определяет два последовательных этапа:
     * <ol>
     *   <li>Сбор "сырых" данных о пробелах в знаниях.</li>
     *   <li>Их анализ, кластеризация и генерация отчета.</li>
     * </ol>
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(fetcherAgent),
                List.of(clustererAgent)
        );
    }
}
