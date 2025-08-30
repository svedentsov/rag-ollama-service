package com.example.ragollama.agent.testanalysis.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Перечисление, определяющее типы тестового технического долга.
 */
@Schema(description = "Тип тестового технического долга")
public enum DebtType {
    /**
     * Отсутствие тестов для производственного кода ("слепая зона").
     */
    MISSING_COVERAGE,

    /**
     * Тест, который был временно отключен (например, аннотацией `@Disabled`).
     */
    DISABLED_TEST,

    /**
     * Тест, выполнение которого занимает неоправданно много времени.
     */
    SLOW_TEST,

    /**
     * Нестабильный ("плавающий") тест, который падает нерегулярно.
     */
    FLAKY_TEST
}
