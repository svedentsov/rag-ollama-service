package com.example.ragollama.agent.architecture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для финального, агрегированного отчета от "AI Architecture Governor".
 *
 * @param overallHealthScore Общая оценка архитектурного здоровья изменений (1-100).
 * @param executiveSummary   Краткое резюме для тимлида/архитектора.
 * @param markdownReport     Готовый к публикации, детализированный отчет в формате Markdown.
 */
@Schema(description = "Агрегированный отчет об архитектурном ревью")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchitecturalReviewReport(
        int overallHealthScore,
        String executiveSummary,
        String markdownReport
) {
}
