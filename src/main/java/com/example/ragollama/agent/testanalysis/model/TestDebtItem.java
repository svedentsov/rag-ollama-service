package com.example.ragollama.agent.testanalysis.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO для представления одного элемента в отчете о тестовом техническом долге.
 * <p>
 * Каждый элемент описывает одну конкретную проблему, найденную в тестовой базе,
 * с указанием ее типа, серьезности и местоположения.
 *
 * @param type        Тип обнаруженного технического долга (например, "Медленный тест").
 * @param severity    Оценка серьезности проблемы.
 * @param location    Точное местоположение проблемы (например, имя класса и метода теста).
 * @param description Человекочитаемое описание проблемы.
 * @param details     Карта с дополнительными количественными метриками по проблеме.
 */
@Schema(description = "Один элемент в отчете о тестовом техническом долге")
public record TestDebtItem(
        DebtType type,
        Severity severity,
        String location,
        String description,
        Map<String, Object> details
) {
}
