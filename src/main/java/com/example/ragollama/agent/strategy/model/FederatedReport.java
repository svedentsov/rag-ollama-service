package com.example.ragollama.agent.strategy.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета с федеративной аналитикой.
 *
 * @param strategicSummary    Высокоуровневое резюме и рекомендации от AI-стратега.
 * @param projectHealthScores Сводная таблица с ключевыми KPI по каждому проекту.
 */
@Schema(description = "Сводный отчет с аналитикой по всем проектам")
public record FederatedReport(
        String strategicSummary,
        List<ProjectHealthSummary> projectHealthScores
) {
}
