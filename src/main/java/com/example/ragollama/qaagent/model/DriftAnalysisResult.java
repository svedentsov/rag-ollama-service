package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для хранения результата статистического анализа дрейфа одного признака.
 *
 * @param featureName Имя признака (колонки).
 * @param psiScore    Рассчитанный Population Stability Index (PSI).
 * @param driftLevel  Категориальная оценка дрейфа (NO_DRIFT, MODERATE_DRIFT, SEVERE_DRIFT).
 * @param details     Дополнительная информация (например, изменение среднего значения).
 */
@Schema(description = "Результат статистического анализа дрейфа для одного признака")
public record DriftAnalysisResult(
        String featureName,
        double psiScore,
        DriftLevel driftLevel,
        String details
) {
    /**
     * Уровни дрейфа на основе PSI.
     */
    public enum DriftLevel {
        NO_DRIFT, MODERATE_DRIFT, SEVERE_DRIFT
    }
}
