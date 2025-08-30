package com.example.ragollama.agent.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для структурированной оценки, которую генерирует LLM.
 * <p>
 * Этот объект содержит **количественные, неденежные** оценки, которые
 * затем используются для детерминированного расчета итоговой стоимости.
 *
 * @param estimatedDevHoursToFix          Оценочное количество часов разработчика на исправление.
 * @param estimatedSupportTicketsPerMonth Оценочное количество обращений в поддержку в месяц из-за этой проблемы.
 * @param potentialUserImpactPercentage   Оценочный процент затронутых пользователей.
 * @param summary                         Краткое резюме и обоснование оценок.
 */
@Schema(description = "Структурированная оценка экономического влияния от LLM")
@JsonIgnoreProperties(ignoreUnknown = true)
public record EconomicImpactAssessment(
        int estimatedDevHoursToFix,
        int estimatedSupportTicketsPerMonth,
        int potentialUserImpactPercentage,
        String summary
) {
}
