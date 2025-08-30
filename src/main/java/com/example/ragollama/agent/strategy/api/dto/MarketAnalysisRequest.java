package com.example.ragollama.agent.strategy.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO для запроса на анализ рыночных возможностей.
 *
 * @param competitorUrl URL сайта или документации конкурента.
 */
@Schema(description = "DTO для запроса на анализ рыночных возможностей")
public record MarketAnalysisRequest(
        @NotBlank String competitorUrl
) {
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("competitorUrl", this.competitorUrl));
    }
}
