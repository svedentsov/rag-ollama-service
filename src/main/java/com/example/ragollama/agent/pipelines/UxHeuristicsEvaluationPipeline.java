package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.ux.domain.UxHeuristicsEvaluatorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для оценки UX по эвристикам Нильсена.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link UxHeuristicsEvaluatorAgent}.
 */
@Component
@RequiredArgsConstructor
public class UxHeuristicsEvaluationPipeline implements AgentPipeline {

    private final UxHeuristicsEvaluatorAgent heuristicsEvaluator;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "ux-heuristics-evaluation-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(List.of(heuristicsEvaluator));
    }
}
