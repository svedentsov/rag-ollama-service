package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.RagOptimizerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для мета-агента "AI MLOps Engineer", который
 * анализирует производительность RAG-системы и предлагает улучшения.
 * <p>
 * Этот конвейер является "фасадом" для сложной бизнес-возможности по
 * автоматической оптимизации. На данный момент он состоит из одного шага,
 * но спроектирован для легкого расширения в будущем.
 */
@Component
@RequiredArgsConstructor
public class RagOptimizerPipeline implements AgentPipeline {

    private final RagOptimizerAgent ragOptimizerAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "rag-optimizer-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним мета-агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(ragOptimizerAgent)
        );
    }
}
