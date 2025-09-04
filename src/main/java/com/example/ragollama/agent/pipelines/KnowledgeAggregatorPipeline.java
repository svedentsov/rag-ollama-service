package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.knowledgegraph.domain.CypherQueryGeneratorAgent;
import com.example.ragollama.agent.knowledgegraph.domain.KnowledgeAggregatorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер, реализующий полный цикл запроса к Графу Знаний.
 */
@Component
@RequiredArgsConstructor
public class KnowledgeAggregatorPipeline implements AgentPipeline {

    private final CypherQueryGeneratorAgent cypherQueryGeneratorAgent;
    private final KnowledgeAggregatorAgent knowledgeAggregatorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "knowledge-aggregator-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Определяет два последовательных этапа: сначала генерация Cypher-запроса,
     * затем его выполнение и синтез ответа.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(cypherQueryGeneratorAgent),
                List.of(knowledgeAggregatorAgent)
        );
    }
}
