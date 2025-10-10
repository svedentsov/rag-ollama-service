package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.analytics.domain.DefectTrendMinerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для анализа и кластеризации исторических данных о дефектах.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link DefectTrendMinerAgent}. Этот агент использует семантический поиск
 * для группировки похожих баг-репортов и LLM для определения общих тем.
 */
@Component
@RequiredArgsConstructor
public class DefectTrendAnalysisPipeline implements AgentPipeline {

    private final DefectTrendMinerAgent trendMiner;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "defect-trend-analysis-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(trendMiner)
        );
    }
}
