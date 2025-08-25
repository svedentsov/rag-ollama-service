package com.example.ragollama.qaagent.model;

/**
 * DTO с детальной информацией об одном нестабильном тесте.
 *
 * @param className     Имя класса теста.
 * @param testName      Имя метода теста.
 * @param totalRuns     Общее количество запусков за период.
 * @param failureCount  Количество падений.
 * @param flakinessRate Рассчитанный процент нестабильности.
 */
public record FlakyTestInfo(
        String className,
        String testName,
        long totalRuns,
        long failureCount,
        double flakinessRate
) {
}
