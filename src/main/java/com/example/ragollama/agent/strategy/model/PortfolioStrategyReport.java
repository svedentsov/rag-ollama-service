package com.example.ragollama.agent.strategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от "Executive Portfolio Governor".
 * Представляет собой высокоуровневый стратегический план.
 *
 * @param quarterlyGoal Главная инженерная цель на следующий квартал.
 * @param initiatives   Список ключевых стратегических инициатив для достижения цели.
 */
@Schema(description = "Стратегический план по портфелю проектов")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortfolioStrategyReport(
        String quarterlyGoal,
        List<StrategicInitiative> initiatives
) {
}
