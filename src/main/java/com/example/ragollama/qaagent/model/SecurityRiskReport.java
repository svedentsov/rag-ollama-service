package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для полного отчета об аудите безопасности.
 *
 * @param findings Список всех обнаруженных проблем безопасности.
 */
@Schema(description = "Отчет об аудите безопасности")
public record SecurityRiskReport(
        List<SecurityFinding> findings
) {
}
