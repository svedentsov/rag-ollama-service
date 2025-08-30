package com.example.ragollama.agent.analytics.model;

/**
 * DTO с полным профилем риска для одного файла, сгенерированным AI-аналитиком.
 *
 * @param filePath               Путь к файлу.
 * @param coveragePercentage     Процент покрытия из JaCoCo.
 * @param historicalFailureCount Количество падений тестов, связанных с этим файлом, в прошлом.
 * @param finalRiskScore         Итоговая оценка риска (от 1 до 10), рассчитанная AI.
 * @param justification          Обоснование оценки от AI.
 */
public record FileRiskProfile(
        String filePath,
        double coveragePercentage,
        long historicalFailureCount,
        int finalRiskScore,
        String justification
) {
}
