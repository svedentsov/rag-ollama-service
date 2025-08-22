package com.example.ragollama.qaagent.web;

/**
 * Упрощенное DTO для десериализации payload'а от Jira Issue webhook.
 */
public record JiraIssuePayload(
        String webhookEvent,
        Issue issue
) {
    public record Issue(String key, Fields fields) {
    }

    public record Fields(String summary, String description, IssueType issuetype) {
    }

    public record IssueType(String name) {
    }
}
