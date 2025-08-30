package com.example.ragollama.agent.dashboard.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для финального отчета от PR Quality Guardian.
 *
 * @param overallScore    Общая оценка качества PR (1-100).
 * @param summary         Резюме от AI Team Lead.
 * @param markdownComment Готовый комментарий для публикации в Pull Request.
 */
@Schema(description = "Агрегированный отчет по качеству Pull Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PRReviewReport(
        int overallScore,
        String summary,
        String markdownComment
) {
}
