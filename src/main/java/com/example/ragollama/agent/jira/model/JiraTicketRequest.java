package com.example.ragollama.agent.jira.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для представления одного тикета, сгенерированного AI-планировщиком.
 *
 * @param title       Заголовок тикета.
 * @param description Подробное описание в формате Markdown.
 * @param issueType   Тип тикета (например, "Bug", "Task", "Story").
 * @param labels      Список меток.
 * @param priority    Приоритет.
 */
@Schema(description = "Структурированное представление задачи для создания в Jira")
@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraTicketRequest(
        String title,
        String description,
        String issueType,
        List<String> labels,
        String priority
) {
}
