package com.example.ragollama.agent.knowledgegaps.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO для запроса на анализ пробелов в знаниях.
 *
 * @param analysisPeriodDays Период для анализа логов в днях.
 */
@Schema(description = "DTO для запроса на анализ пробелов в знаниях")
public record KnowledgeGapAnalysisRequest(
        @Schema(description = "Период для анализа в днях", defaultValue = "30")
        @NotNull @Min(1) @Max(365)
        Integer analysisPeriodDays
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска конвейера.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("analysisPeriodDays", this.analysisPeriodDays));
    }
}
