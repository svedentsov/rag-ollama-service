package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.executive.domain.UserFeedbackFetcherAgent;
import com.example.ragollama.agent.strategy.domain.ProductPortfolioStrategistAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для "AI CPO", формирующего продуктовую стратегию.
 * <p>
 * В этой упрощенной версии конвейер последовательно собирает фидбэк,
 * а затем передает его стратегу. В более сложной реализации сбор фидбэка
 * и анализ конкурентов могли бы идти параллельно.
 */
@Component
@RequiredArgsConstructor
public class ProductStrategyPipeline implements AgentPipeline {

    private final UserFeedbackFetcherAgent userFeedbackFetcher;
    // private final MarketOpportunityPipeline marketOpportunity; // Could be a sub-pipeline
    private final ProductPortfolioStrategistAgent strategist;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "product-strategy-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(userFeedbackFetcher), // Add market opportunity agent here for parallel execution
                List.of(strategist)
        );
    }
}
