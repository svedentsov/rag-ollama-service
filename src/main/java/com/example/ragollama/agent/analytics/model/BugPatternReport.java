package com.example.ragollama.agent.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от Bug Pattern Detector.
 *
 * @param patterns Список обнаруженных системных паттернов багов.
 */
@Schema(description = "Отчет об обнаруженных системных паттернах багов")
@JsonIgnoreProperties(ignoreUnknown = true)
public record BugPatternReport(
        List<BugPattern> patterns
) {
}
