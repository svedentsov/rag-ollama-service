package com.example.ragollama.agent.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для описания одного обнаруженного паттерна багов.
 *
 * @param patternTitle   Название паттерна, сгенерированное AI.
 * @param description    Подробное описание первопричины.
 * @param severity       Оценка серьезности системной проблемы.
 * @param recommendation Предлагаемое системное решение.
 * @param exampleBugIds  Список ID багов, которые являются примерами этого паттерна.
 */
@Schema(description = "Описание одного обнаруженного паттерна багов")
@JsonIgnoreProperties(ignoreUnknown = true)
public record BugPattern(
        String patternTitle,
        String description,
        String severity,
        String recommendation,
        List<String> exampleBugIds
) {
}
