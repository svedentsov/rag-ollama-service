package com.example.ragollama.qaagent.tools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO для десериализации ответа от Jira API при запросе деталей задачи.
 * <p>
 * Содержит только те поля, которые необходимы для работы QA-агентов.
 * Аннотация {@code @JsonIgnoreProperties(ignoreUnknown = true)} делает
 * парсинг устойчивым к наличию других, неиспользуемых полей в JSON.
 *
 * @param key    Ключ задачи (например, "PROJ-123").
 * @param fields Поля задачи (описание, статус и т.д.).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssueDto(
        String key,
        Fields fields
) {
    /**
     * Вложенный DTO для полей задачи.
     *
     * @param summary     Заголовок.
     * @param description Описание.
     * @param status      Объект статуса.
     * @param issuetype   Объект типа задачи.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fields(
            String summary,
            String description,
            Status status,
            IssueType issuetype
    ) {
    }

    /**
     * DTO для статуса задачи.
     *
     * @param name Имя статуса (например, "In Progress").
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(String name) {
    }

    /**
     * DTO для типа задачи.
     *
     * @param name Имя типа (например, "Bug").
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IssueType(String name) {
    }
}
