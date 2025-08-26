package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для финального отчета об экономической оценке дефекта/техдолга.
 *
 * @param subject                Объект анализа (например, путь к файлу).
 * @param costOfRemediation      Рассчитанная стоимость исправления.
 * @param costOfInactionPerMonth Рассчитанная стоимость бездействия в месяц.
 * @param summary                Резюме и рекомендации, сгенерированные LLM.
 * @param assessmentDetails      Детализированная оценка от LLM, на основе которой были сделаны расчеты.
 */
@Schema(description = "Отчет об экономической оценке дефекта или технического долга")
public record DefectEconomicReport(
        String subject,
        double costOfRemediation,
        double costOfInactionPerMonth,
        String summary,
        EconomicImpactAssessment assessmentDetails
) {
}
