package com.example.ragollama.evaluation.model;

import java.util.List;
import java.util.Map;

/**
 * Содержит результаты полного прогона оценки по "золотому датасету".
 *
 * @param totalRecords       Общее количество обработанных записей.
 * @param retrievalPrecision Средняя точность поиска (доля найденных релевантных среди всех найденных).
 * @param retrievalRecall    Средняя полнота поиска (доля найденных релевантных среди всех ожидаемых).
 * @param retrievalF1Score   Среднее гармоническое F1-мера для поиска.
 * @param failures           Список ID запросов, которые не удалось обработать.
 * @param details            Детальная статистика по каждому запросу.
 */
public record EvaluationResult(
        int totalRecords,
        double retrievalPrecision,
        double retrievalRecall,
        double retrievalF1Score,
        List<String> failures,
        Map<String, RecordResult> details
) {
    /**
     * Результаты оценки для одной записи.
     */
    public record RecordResult(
            double precision,
            double recall,
            int expectedCount,
            int retrievedCount,
            int intersectionCount
    ) {
    }
}
