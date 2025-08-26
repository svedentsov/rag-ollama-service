package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для описания одного конкретного риска.
 *
 * @param area        Область риска (например, "Test Stability", "Code Quality").
 * @param severity    Серьезность риска ("Critical", "High", "Medium", "Low").
 * @param description Описание риска.
 */
@Schema(description = "Описание одного фактора риска")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskFactor(
        String area,
        String severity,
        String description
) {
}
