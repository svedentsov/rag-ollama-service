package com.example.ragollama.agent.executive.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Унифицированный DTO для представления одного нарушения любой политики.
 *
 * @param sourceAgent    Агент, обнаруживший нарушение.
 * @param policyArea     Область политики (например, "Security", "Architecture").
 * @param severity       Серьезность нарушения.
 * @param description    Описание проблемы.
 * @param filePath       Файл, в котором обнаружена проблема.
 * @param recommendation Предложение по исправлению.
 */
@Schema(description = "Одно унифицированное нарушение политики")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyViolation(
        String sourceAgent,
        String policyArea,
        String severity,
        String description,
        String filePath,
        String recommendation
) {
}
