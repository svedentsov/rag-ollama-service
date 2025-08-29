package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для отчета от Market Opportunity Analyzer.
 *
 * @param summary              Краткое резюме анализа.
 * @param featureGaps          Список фичей, которые есть у конкурента, но нет у нас.
 * @param strategicOpportunity Главная стратегическая возможность, выявленная AI.
 */
@Schema(description = "Отчет об анализе рыночных возможностей")
@JsonIgnoreProperties(ignoreUnknown = true)
public record FeatureGapReport(
        String summary,
        List<String> featureGaps,
        String strategicOpportunity
) {
}
