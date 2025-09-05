package com.example.ragollama.agent.knowledgegaps.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от агента анализа пробелов в знаниях.
 *
 * @param summary         Краткое резюме от AI-аналитика.
 * @param suggestedTopics Список приоритизированных тем для создания документации.
 */
@Schema(description = "Отчет о пробелах в базе знаний с предложениями по их устранению")
@JsonIgnoreProperties(ignoreUnknown = true)
public record KnowledgeGapReport(
        String summary,
        List<KnowledgeGapTheme> suggestedTopics
) {
}
