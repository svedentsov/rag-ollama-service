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
 * DTO для запроса на генерацию полного Executive Dashboard.
 * Агрегирует все параметры, необходимые для запуска всех губернаторских конвейеров.
 *
 * @param analysisPeriodDays Период для анализа исторических данных.
 * @param competitorUrls     Список URL конкурентов для анализа рынка.
 */
@Schema(description = "DTO для запроса на генерацию полного Executive Dashboard")
public record ExecutiveDashboardRequest(
        @Schema(description = "Период для анализа исторических данных в днях", defaultValue = "90")
        @NotNull @Min(30) @Max(365)
        Integer analysisPeriodDays,

        @Schema(description = "Список URL сайтов конкурентов для анализа", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        List<@URL String> competitorUrls
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейеры.
     *
     * @return Контекст для запуска.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "analysisPeriodDays", this.analysisPeriodDays,
                "days", this.analysisPeriodDays, // For compatibility
                "competitorUrls", this.competitorUrls
        ));
    }
}
