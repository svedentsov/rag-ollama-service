package com.example.ragollama.agent.executive.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от "Architectural Evolution Governor".
 */
@Schema(description = "Отчет о долгосрочном здоровье архитектуры")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchitecturalHealthReport(
        int overallHealthScore,
        String executiveSummary,
        List<ArchitecturalFinding> keyFindings
) {
    /**
     * DTO для одного архитектурного наблюдения.
     */
    @Schema(description = "Одно ключевое архитектурное наблюдение")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArchitecturalFinding(
            String type, // e.g., "High Coupling", "Technology Lag", "Maintenance Hotspot"
            String severity, // "High", "Medium", "Low"
            String description,
            String recommendation
    ) {
    }
}
