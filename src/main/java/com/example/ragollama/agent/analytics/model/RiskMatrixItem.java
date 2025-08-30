package com.example.ragollama.agent.analytics.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO для представления одного элемента на матрице рисков.
 *
 * @param filePath        Путь к измененному файлу.
 * @param likelihoodScore Оценка вероятности возникновения дефекта (1-5).
 * @param impactScore     Оценка влияния дефекта на бизнес/пользователей (1-5).
 * @param justification   Детальное обоснование оценок из предыдущих отчетов.
 */
@Schema(description = "Один элемент на матрице рисков")
public record RiskMatrixItem(
        String filePath,
        int likelihoodScore,
        int impactScore,
        Map<String, Object> justification
) {
}
