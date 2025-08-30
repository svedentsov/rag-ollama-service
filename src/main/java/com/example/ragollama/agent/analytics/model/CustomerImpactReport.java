package com.example.ragollama.agent.analytics.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для структурированного отчета об анализе влияния на пользователей.
 */
@Schema(description = "Отчет об анализе влияния на пользователей")
public record CustomerImpactReport(
        List<CustomerImpactAnalysis> analyses
) {
}
