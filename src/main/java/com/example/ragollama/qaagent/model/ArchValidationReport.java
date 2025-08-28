package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от агента проверки архитектурной консистентности.
 *
 * @param overallStatus Общий вердикт: CONSISTENT или INCONSISTENT.
 * @param violations    Список обнаруженных архитектурных нарушений.
 */
@Schema(description = "Отчет о проверке архитектурной консистентности")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchValidationReport(
        @Schema(description = "Общий вердикт о консистентности")
        String overallStatus,
        @Schema(description = "Список обнаруженных архитектурных нарушений")
        List<ArchViolation> violations
) {
}
