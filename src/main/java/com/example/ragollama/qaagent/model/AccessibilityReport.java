package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального, обогащенного AI отчета об аудите доступности (a11y).
 * <p>
 * Этот объект агрегирует как "сырые" данные от сканера, так и
 * человекочитаемое резюме и рекомендации, сгенерированные LLM.
 *
 * @param summary            Высокоуровневое резюме, сгенерированное LLM.
 * @param violations         Полный список технических нарушений, обнаруженных сканером.
 * @param topRecommendations Список наиболее приоритетных рекомендаций от LLM.
 */
@Schema(description = "Финальный отчет об аудите доступности (a11y)")
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccessibilityReport(
        @Schema(description = "Резюме и общая оценка от AI")
        String summary,
        @Schema(description = "Список приоритетных рекомендаций по исправлению")
        List<String> topRecommendations,
        @Schema(description = "Полный список технических нарушений")
        List<AccessibilityViolation> violations
) {
}
