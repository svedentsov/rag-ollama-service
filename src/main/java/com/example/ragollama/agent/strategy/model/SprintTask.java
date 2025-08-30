package com.example.ragollama.agent.strategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного элемента в бэклоге спринта.
 * По сути, это готовая задача для создания в Jira.
 *
 * @param title       Заголовок тикета.
 * @param description Подробное описание задачи.
 * @param priority    Приоритет ("High", "Highest").
 */
@Schema(description = "Одна задача в плане на спринт")
@JsonIgnoreProperties(ignoreUnknown = true)
public record SprintTask(
        String title,
        String description,
        String priority
) {
}
