package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.strategy.domain.FeatureGapAnalysisAgent;
import com.example.ragollama.agent.web.domain.WebCrawlerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для мета-агента "AI Product Strategist", который
 * анализирует рыночные возможности.
 */
@Component
@RequiredArgsConstructor
public class MarketOpportunityPipeline implements AgentPipeline {
    private final WebCrawlerAgent webCrawler;
    private final FeatureGapAnalysisAgent featureGapAnalyzer;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "market-opportunity-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два последовательных этапа: сначала сбор данных с сайта конкурента,
     * затем их анализ.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(webCrawler),
                List.of(featureGapAnalyzer)
        );
    }
}
