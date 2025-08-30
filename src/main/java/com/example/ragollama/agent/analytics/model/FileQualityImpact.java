package com.example.ragollama.agent.analytics.model;

import com.example.ragollama.shared.model.codeanalysis.CodeMetrics;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления результата анализа одного файла.
 *
 * @param filePath               Путь к файлу.
 * @param codeMetrics            Результаты статического анализа.
 * @param historicalFailureCount Количество падений тестов, связанных с этим файлом, в прошлом.
 * @param maintainabilityRisk    Оценка риска для поддержки (1-10).
 * @param defectProbability      Вероятность появления дефектов (1-10).
 * @param justification          Обоснование оценок, сгенерированное LLM.
 */
@Schema(description = "Оценка влияния качества кода для одного файла")
@JsonIgnoreProperties(ignoreUnknown = true)
public record FileQualityImpact(
        String filePath,
        CodeMetrics codeMetrics,
        long historicalFailureCount,
        int maintainabilityRisk,
        int defectProbability,
        String justification
) {
}
