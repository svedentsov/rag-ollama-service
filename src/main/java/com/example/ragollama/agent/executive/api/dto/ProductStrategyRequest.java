package com.example.ragollama.agent.executive.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

import java.util.List;
import java.util.Map;

/**
 * DTO для запроса на генерацию продуктовой стратегии.
 *
 * @param competitorUrls     Список URL сайтов конкурентов для анализа.
 * @param analysisPeriodDays Период для анализа обратной связи от пользователей.
 */
@Schema(description = "DTO для запроса на генерацию продуктовой стратегии")
public record ProductStrategyRequest(
        @Schema(description = "Список URL сайтов конкурентов для анализа", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        List<@URL String> competitorUrls,

        @Schema(description = "Период для анализа обратной связи в днях", defaultValue = "90")
        @NotNull @Min(30) @Max(365)
        Integer analysisPeriodDays
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в планировщик.
     *
     * @return Контекст для запуска.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "competitorUrls", this.competitorUrls,
                "analysisPeriodDays", this.analysisPeriodDays
        ));
    }
}
