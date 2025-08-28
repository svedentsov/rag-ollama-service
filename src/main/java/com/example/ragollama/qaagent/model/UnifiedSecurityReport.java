package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального, агрегированного отчета по безопасности.
 *
 * @param overallRiskScore    Общая оценка риска (1-100), рассчитанная AI.
 * @param executiveSummary    Резюме для руководства.
 * @param prioritizedFindings Список всех находок, отсортированный по критичности.
 */
@Schema(description = "Единый, приоритизированный отчет по безопасности")
@JsonIgnoreProperties(ignoreUnknown = true)
public record UnifiedSecurityReport(
        int overallRiskScore,
        String executiveSummary,
        List<SecurityFinding> prioritizedFindings
) {
}
