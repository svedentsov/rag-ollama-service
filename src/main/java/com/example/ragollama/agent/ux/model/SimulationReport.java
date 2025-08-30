package com.example.ragollama.agent.ux.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета о симуляции поведения пользователя.
 *
 * @param goal         Исходная цель, поставленная перед агентом.
 * @param steps        Список всех выполненных шагов (команд).
 * @param finalOutcome Результат симуляции (SUCCESS, FAILURE).
 * @param summary      Итоговое резюме от AI.
 */
@Schema(description = "Отчет о симуляции поведения пользователя")
public record SimulationReport(
        String goal,
        List<AgentCommand> steps,
        String finalOutcome,
        String summary
) {
}
