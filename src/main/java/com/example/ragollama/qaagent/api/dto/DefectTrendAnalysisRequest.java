package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * DTO для запроса на анализ трендов в дефектах.
 *
 * @param days             Период для анализа в днях.
 * @param clusterThreshold Порог схожести для включения бага в кластер.
 */
@Schema(description = "DTO для запроса на анализ трендов в дефектах")
public record DefectTrendAnalysisRequest(
        @Schema(description = "Количество последних дней для анализа", requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "30")
        @Min(1) @Max(90) @NotNull
        Integer days,

        @Schema(description = "Порог схожести для кластеризации (0.1-1.0)", defaultValue = "0.85")
        @Min(0) @Max(1)
        Double clusterThreshold
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "days", days,
                "clusterThreshold", clusterThreshold != null ? clusterThreshold : 0.85
        ));
    }
}
