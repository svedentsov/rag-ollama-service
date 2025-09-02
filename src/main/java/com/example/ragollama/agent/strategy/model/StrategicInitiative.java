package com.example.ragollama.agent.strategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для описания одной стратегической инициативы.
 *
 * @param title           Название инициативы (например, "Инициатива по повышению стабильности CI/CD").
 * @param problem         Описание проблемы, которую решает инициатива.
 * @param expectedOutcome Ожидаемый бизнес-результат.
 * @param keyProjects     Ключевые проекты, вовлеченные в инициативу.
 * @param successKpis     Ключевые показатели эффективности для измерения успеха.
 */
@Schema(description = "Описание одной стратегической инициативы")
@JsonIgnoreProperties(ignoreUnknown = true)
public record StrategicInitiative(
        String title,
        String problem,
        String expectedOutcome,
        List<String> keyProjects,
        List<String> successKpis
) {
}
