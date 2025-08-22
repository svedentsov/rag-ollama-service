package com.example.ragollama.qaagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Клиент для взаимодействия с GitHub API v3.
 * Использует WebClient для асинхронных, неблокирующих запросов.
 */
@Slf4j
@Service
public class GitHubApiClient {

    private final WebClient webClient;
    private final String githubApiToken;

    public GitHubApiClient(WebClient.Builder webClientBuilder,
                           @Value("${app.integrations.github.api-token}") String githubApiToken) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
        this.githubApiToken = githubApiToken;
    }

    /**
     * Загружает diff для указанного Pull Request.
     *
     * @param owner      Владелец репозитория.
     * @param repo       Имя репозитория.
     * @param pullNumber Номер Pull Request.
     * @return {@link Mono} со строковым представлением diff.
     */
    public Mono<String> getPullRequestDiff(String owner, String repo, int pullNumber) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}", owner, repo, pullNumber)
                .header(HttpHeaders.ACCEPT, "application/vnd.github.v3.diff")
                .headers(h -> h.setBearerAuth(githubApiToken))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("Ошибка при загрузке diff для PR #{}: {}", pullNumber, e.getMessage()));
    }

    /**
     * Публикует комментарий в Pull Request.
     *
     * @param owner      Владелец репозитория.
     * @param repo       Имя репозитория.
     * @param pullNumber Номер Pull Request.
     * @param comment    Текст комментария.
     * @return {@link Mono}, который завершается при успешной публикации.
     */
    public Mono<Void> postCommentToPullRequest(String owner, String repo, int pullNumber, String comment) {
        return webClient.post()
                .uri("/repos/{owner}/{repo}/issues/{issueNumber}/comments", owner, repo, pullNumber)
                .headers(h -> h.setBearerAuth(githubApiToken))
                .bodyValue(Map.of("body", comment))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Комментарий успешно опубликован в PR #{}", pullNumber))
                .doOnError(e -> log.error("Ошибка при публикации комментария в PR #{}: {}", pullNumber, e.getMessage()));
    }
}
