package com.example.ragollama.agent.compliance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от SCA Compliance Agent.
 *
 * @param overallStatus Общий вердикт: COMPLIANT или NON_COMPLIANT.
 * @param summary       Резюме от AI-аналитика.
 * @param violations    Список обнаруженных нарушений лицензионной политики.
 */
@Schema(description = "Финальный отчет по анализу лицензий зависимостей")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScaReport(
        String overallStatus,
        String summary,
        List<LicenseViolation> violations
) {
}
