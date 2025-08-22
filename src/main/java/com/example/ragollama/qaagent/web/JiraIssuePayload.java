package com.example.ragollama.qaagent.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Упрощенное DTO для десериализации payload'а от Jira Issue webhook.
 * <p>
 * Содержит только поля, необходимые для работы агента-анализатора багов.
 * Аннотация {@code @JsonIgnoreProperties(ignoreUnknown = true)} делает
 * парсинг устойчивым к наличию других, неиспользуемых полей в JSON.
 *
 * @param webhookEvent Тип события (например, "jira:issue_created").
 * @param issue        Объект с деталями задачи.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssuePayload(
        String webhookEvent,
        Issue issue
) {
    /**
     * Детали задачи.
     *
     * @param key    Ключ задачи (например, "PROJ-123").
     * @param fields Поля задачи.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Issue(String key, Fields fields) {
    }

    /**
     * Поля задачи.
     *
     * @param summary     Заголовок задачи.
     * @param description Описание задачи.
     * @param issuetype   Тип задачи.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fields(String summary, String description, IssueType issuetype) {
    }

    /**
     * Тип задачи.
     *
     * @param name Имя типа (например, "Bug").
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IssueType(String name) {
    }
}
