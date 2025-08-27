package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для представления одного нарушения доступности, обнаруженного сканером.
 * <p>
 * Структура этого объекта соответствует типичному выводу инструментов,
 * таких как Axe-core.
 *
 * @param id          Уникальный идентификатор правила (например, "color-contrast").
 * @param severity    Серьезность нарушения ("critical", "serious", "moderate", "minor").
 * @param description Описание проблемы.
 * @param helpUrl     Ссылка на документацию с подробным объяснением правила.
 * @param nodes       Список HTML-селекторов элементов, где была найдена проблема.
 */
@Schema(description = "Одно техническое нарушение доступности (a11y)")
public record AccessibilityViolation(
        String id,
        String severity,
        String description,
        String helpUrl,
        List<String> nodes
) {
}
