package com.example.ragollama.agent.testanalysis.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для полного отчета о тестовом техническом долге.
 * <p>
 * Агрегирует все найденные проблемы и содержит сгенерированное LLM резюме,
 * которое помогает приоритизировать усилия по их устранению.
 *
 * @param summary Сгенерированное LLM резюме (Executive Summary) отчета.
 * @param items   Детальный список всех обнаруженных элементов технического долга.
 */
@Schema(description = "Полный отчет о тестовом техническом долге")
public record TestDebtReport(
        String summary,
        List<TestDebtItem> items
) {
}
