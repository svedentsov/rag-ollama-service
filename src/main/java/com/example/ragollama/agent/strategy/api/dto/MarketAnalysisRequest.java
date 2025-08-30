package com.example.ragollama.agent.strategy.api.dto;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.web.domain.WebCrawlerAgent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.util.Map;

/**
 * DTO для запроса на анализ рыночных возможностей.
 *
 * @param competitorUrl URL сайта или документации конкурента для анализа.
 */
@Schema(description = "DTO для запроса на анализ рыночных возможностей")
public record MarketAnalysisRequest(
        @Schema(description = "URL сайта или документации конкурента", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://competitor.com/features")
        @NotBlank
        @URL
        String competitorUrl
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(WebCrawlerAgent.URL_KEY, this.competitorUrl));
    }
}
