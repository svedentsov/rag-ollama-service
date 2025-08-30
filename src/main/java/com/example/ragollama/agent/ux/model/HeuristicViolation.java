package com.example.ragollama.agent.ux.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для описания одного нарушения эвристики юзабилити.
 *
 * @param heuristicViolated       Название нарушенной эвристики.
 * @param severity                Серьезность проблемы ("High", "Medium", "Low").
 * @param description             Объяснение, как это влияет на пользователя.
 * @param violatingElementSnippet Фрагмент HTML-кода, где найдена проблема.
 * @param recommendation          Предложение по исправлению.
 */
@Schema(description = "Описание одного нарушения эвристики юзабилити")
@JsonIgnoreProperties(ignoreUnknown = true)
public record HeuristicViolation(
        String heuristicViolated,
        String severity,
        String description,
        String violatingElementSnippet,
        String recommendation
) {
}
