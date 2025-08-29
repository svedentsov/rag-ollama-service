package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления результата аудита покрытия для одного файла.
 *
 * @param filePath           Путь к файлу.
 * @param coveragePercentage Процент покрытия методов (от 0.0 до 100.0), полученный из JaCoCo.
 * @param riskLevel          Вычисленный уровень риска.
 * @param summary            Краткое описание риска.
 */
@Schema(description = "Результат аудита покрытия для одного файла")
public record FileCoverageRisk(
        String filePath,
        double coveragePercentage,
        RiskLevel riskLevel,
        String summary
) {
    /**
     * Перечисление уровней риска.
     */
    public enum RiskLevel {
        HIGH, MEDIUM, LOW, INFO
    }
}
