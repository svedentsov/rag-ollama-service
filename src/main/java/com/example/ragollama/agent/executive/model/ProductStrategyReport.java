package com.example.ragollama.agent.executive.model;

import com.example.ragollama.agent.strategy.model.StrategicInitiative;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для отчета от "Product Portfolio Strategist".
 *
 * @param quarterlyGoal Главная продуктовая цель на квартал.
 * @param initiatives   Ключевые продуктовые инициативы.
 */
@Schema(description = "Стратегический план по развитию продуктового портфеля")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductStrategyReport(
        String quarterlyGoal,
        List<StrategicInitiative> initiatives
) {
}
