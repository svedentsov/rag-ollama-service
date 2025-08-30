package com.example.ragollama.agent.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления результата анализа для одного файла или компонента.
 *
 * @param impactedUserGroup Группа пользователей, на которую влияет изменение.
 * @param impactArea        Область продукта, затронутая изменением.
 * @param severity          Серьезность влияния.
 * @param summary           Резюме изменения с точки зрения пользователя.
 * @param recommendation    Рекомендуемые действия.
 */
@Schema(description = "Результат анализа влияния одного изменения")
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerImpactAnalysis(
        String impactedUserGroup,
        String impactArea,
        String severity,
        String summary,
        String recommendation
) {
}
