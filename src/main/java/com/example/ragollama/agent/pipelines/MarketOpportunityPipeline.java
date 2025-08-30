package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.strategy.domain.FeatureGapAnalysisAgent;
import com.example.ragollama.agent.web.domain.WebCrawlerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class MarketOpportunityPipeline implements AgentPipeline {
    private final WebCrawlerAgent webCrawler;
    private final FeatureGapAnalysisAgent featureGapAnalyzer;

    @Override
    public String getName() {
        return "market-opportunity-pipeline";
    }

    @Override
    public List<QaAgent> getAgents() {
        return List.of(webCrawler, featureGapAnalyzer);
    }
}
