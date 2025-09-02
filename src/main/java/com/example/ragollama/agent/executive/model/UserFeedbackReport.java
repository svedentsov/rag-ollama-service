package com.example.ragollama.agent.executive.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для отчета от UserFeedbackClustererAgent.
 *
 * @param summary        Краткий вывод о главных проблемах пользователей.
 * @param feedbackThemes Список кластеризованных тем обратной связи.
 */
@Schema(description = "Отчет о кластеризации обратной связи от пользователей")
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserFeedbackReport(
        String summary,
        List<FeedbackTheme> feedbackThemes
) {
    /**
     * DTO для одной темы (кластера) обратной связи.
     */
    @Schema(description = "Одна тема (кластер) обратной связи")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeedbackTheme(
            String theme,
            int frequency,
            String userImpact,
            List<String> exampleQuotes
    ) {
    }
}
