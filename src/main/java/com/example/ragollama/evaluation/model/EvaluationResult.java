package com.example.ragollama.evaluation.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * DTO, содержащий полные результаты прогона оценки по "золотому датасету".
 *
 * @param totalRecords       Общее количество обработанных записей.
 * @param retrievalPrecision Средняя точность поиска (доля найденных релевантных среди всех найденных).
 * @param retrievalRecall    Средняя полнота поиска (доля найденных релевантных среди всех ожидаемых).
 * @param retrievalF1Score   Среднее гармоническое F1-мера для поиска, ключевая метрика качества.
 * @param failures           Список ID запросов, которые не удалось обработать из-за ошибок.
 * @param details            Детальная статистика по каждому успешно обработанному запросу.
 */
@Schema(description = "Результаты полного прогона оценки RAG по 'золотому датасету'")
public record EvaluationResult(
        int totalRecords,
        double retrievalPrecision,
        double retrievalRecall,
        double retrievalF1Score,
        List<String> failures,
        Map<String, RecordResult> details
) {
    /**
     * Результаты оценки для одной записи (одного запроса) из датасета.
     *
     * @param precision         Точность для данного запроса.
     * @param recall            Полнота для данного запроса.
     * @param expectedCount     Количество ожидаемых документов.
     * @param retrievedCount    Количество фактически найденных документов.
     * @param intersectionCount Количество правильно найденных документов (True Positives).
     */
    @Schema(description = "Результаты оценки для одной записи из датасета")
    public record RecordResult(
            double precision,
            double recall,
            int expectedCount,
            int retrievedCount,
            int intersectionCount
    ) {
    }
}
