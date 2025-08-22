package com.example.ragollama.qaagent.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Упрощенное DTO для десериализации payload'а от GitHub Pull Request webhook.
 */
public record GitHubPullRequestPayload(
        String action,
        int number,
        @JsonProperty("pull_request") PullRequest pullRequest,
        Repository repository
) {
    public record PullRequest(String diff_url, Head head) {
    }

    public record Repository(String name, Owner owner) {
    }

    public record Owner(String login) {
    }

    public record Head(String ref, String sha) {
    }
}
