package com.example.ragollama.agent.architecture.model;

/**
 * DTO для представления одного узла (компонента) в графе зависимостей.
 *
 * @param id    Уникальный идентификатор узла (например, имя класса).
 * @param label Метка, отображаемая на диаграмме.
 */
public record ComponentNode(String id, String label) {
}
