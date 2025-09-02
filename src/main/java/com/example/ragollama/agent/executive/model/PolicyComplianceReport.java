package com.example.ragollama.agent.executive.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального, агрегированного отчета от "Policy & Safety Governor".
 *
 * @param overallStatus         Финальный, машиночитаемый вердикт.
 * @param executiveSummary      Краткое резюме для тимлида.
 * @param prioritizedViolations Список всех найденных нарушений, отсортированный по критичности.
 */
@Schema(description = "Единый отчет о соответствии политикам")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyComplianceReport(
        ComplianceStatus overallStatus,
        String executiveSummary,
        List<PolicyViolation> prioritizedViolations
) {
    public enum ComplianceStatus {
        COMPLIANT,
        NON_COMPLIANT
    }
}
