package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от агента-оптимизатора.
 *
 * @param summary         Высокоуровневое резюме анализа от AI.
 * @param recommendations Список конкретных, приоритизированных рекомендаций по улучшению.
 */
@Schema(description = "Отчет с рекомендациями по оптимизации RAG-конвейера")
@JsonIgnoreProperties(ignoreUnknown = true)
public record OptimizationReport(
        @Schema(description = "Резюме анализа и ключевые выводы")
        String summary,
        @Schema(description = "Список конкретных рекомендаций по изменению конфигурации")
        List<OptimizationRecommendation> recommendations
) {
}
