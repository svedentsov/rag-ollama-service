package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от XAI Test Oracle.
 *
 * @param traceabilityMatrix Список связей между сгенерированными тестами и требованиями.
 * @param coverageGaps       Список требований, которые не были покрыты тестами.
 * @param overallAssessment  Общая оценка полноты и качества тестового набора от AI.
 */
@Schema(description = "Отчет от XAI Test Oracle с анализом тестового покрытия")
@JsonIgnoreProperties(ignoreUnknown = true)
public record TestOracleReport(
        @Schema(description = "Матрица трассируемости от теста к требованию")
        List<TraceabilityLink> traceabilityMatrix,
        @Schema(description = "Требования, не покрытые тестами")
        List<String> coverageGaps,
        @Schema(description = "Общая оценка тестового набора от AI")
        String overallAssessment
) {
}
