package com.example.ragollama.agent.git.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Клиент для взаимодействия с GitHub API v3.
 * <p>
 * Использует неблокирующий {@link WebClient} и полностью асинхронную модель
 * для выполнения HTTP-запросов. Конфигурация клиента (URL, токен)
 * управляется через {@code application.yml}.
 */
@Slf4j
@Service
public class GitHubApiClient {

    private final WebClient webClient;
    private final String githubApiToken;

    /**
     * Конструктор, который создает и настраивает {@link WebClient}.
     *
     * @param webClientBuilder Строитель {@link WebClient}, предварительно
     *                         настроенный в {@link com.example.ragollama.shared.config.AppConfig}.
     * @param githubApiToken   Персональный токен доступа (PAT) для аутентификации.
     */
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
     * Асинхронно загружает diff для указанного Pull Request в текстовом формате.
     *
     * @param owner      Владелец репозитория.
     * @param repo       Имя репозитория.
     * @param pullNumber Номер Pull Request.
     * @return {@link Mono} со строковым представлением diff.
     */
    public Mono<String> getPullRequestDiff(String owner, String repo, int pullNumber) {
        log.debug("Запрос diff для PR #{} в репозитории {}/{}", pullNumber, owner, repo);
        return webClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}", owner, repo, pullNumber)
                // Запрашиваем специальный media type для получения diff
                .header(HttpHeaders.ACCEPT, "application/vnd.github.v3.diff")
                .headers(h -> h.setBearerAuth(githubApiToken))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("Ошибка при загрузке diff для PR #{}: {}", pullNumber, e.getMessage()));
    }

    /**
     * Асинхронно публикует комментарий в Pull Request.
     *
     * @param owner      Владелец репозитория.
     * @param repo       Имя репозитория.
     * @param pullNumber Номер Pull Request (используется как issue number для API комментариев).
     * @param comment    Текст комментария.
     * @return {@link Mono}, который завершается при успешной публикации.
     */
    public Mono<Void> postCommentToPullRequest(String owner, String repo, int pullNumber, String comment) {
        log.info("Публикация комментария в PR #{}...", pullNumber);
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
