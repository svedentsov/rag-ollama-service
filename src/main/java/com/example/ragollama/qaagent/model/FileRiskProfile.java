package com.example.ragollama.qaagent.model;

/**
 * DTO с полным профилем риска для одного файла.
 */
public record FileRiskProfile(
        String filePath,
        double coveragePercentage,
        long historicalFailureCount,
        int finalRiskScore, // от 1 до 10
        String justification
) {
}