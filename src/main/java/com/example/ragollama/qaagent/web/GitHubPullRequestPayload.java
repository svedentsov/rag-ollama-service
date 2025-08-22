package com.example.ragollama.qaagent.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Упрощенное DTO для десериализации payload'а от GitHub Pull Request webhook.
 * <p>
 * Содержит только те поля, которые необходимы для работы нашего конвейера.
 * Использование `record` делает код лаконичным и неизменяемым.
 *
 * @param action      Тип действия (например, "opened", "synchronize").
 * @param number      Номер Pull Request.
 * @param pullRequest Объект с деталями PR.
 * @param repository  Объект с информацией о репозитории.
 */
public record GitHubPullRequestPayload(
        String action,
        int number,
        @JsonProperty("pull_request") PullRequest pullRequest,
        Repository repository
) {
    /**
     * Детали Pull Request.
     *
     * @param diff_url URL для получения diff.
     * @param head     Информация о ветке-источнике.
     */
    public record PullRequest(String diff_url, Head head) {
    }

    /**
     * Информация о репозитории.
     *
     * @param name  Имя репозитория.
     * @param owner Владелец репозитория.
     */
    public record Repository(String name, Owner owner) {
    }

    /**
     * Владелец репозитория.
     *
     * @param login Имя (логин) владельца.
     */
    public record Owner(String login) {
    }

    /**
     * Ветка-источник.
     *
     * @param ref Имя ветки.
     * @param sha SHA-хэш последнего коммита.
     */
    public record Head(String ref, String sha) {
    }
}
