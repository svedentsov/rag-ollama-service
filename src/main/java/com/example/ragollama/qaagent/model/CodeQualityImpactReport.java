package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для полного отчета об оценке влияния качества кода.
 *
 * @param riskProfiles Список профилей риска для каждого проанализированного файла.
 */
@Schema(description = "Отчет об оценке влияния качества кода")
public record CodeQualityImpactReport(
        List<FileQualityImpact> riskProfiles
) {
}
