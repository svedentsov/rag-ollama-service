package com.example.ragollama.qaagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Клиент для взаимодействия с Jira Cloud REST API.
 * <p>
 * Инкапсулирует логику HTTP-запросов к Jira, используя неблокирующий
 * {@link WebClient}.
 */
@Slf4j
@Service
public class JiraApiClient {

    private final WebClient webClient;

    /**
     * Конструктор, который создает и настраивает {@link WebClient} для Jira.
     *
     * @param webClientBuilder Строитель {@link WebClient} из общей конфигурации.
     * @param baseUrl          Базовый URL вашего инстанса Jira.
     * @param apiUser          Email пользователя для аутентификации.
     * @param apiToken         API токен, сгенерированный в Jira.
     */
    public JiraApiClient(WebClient.Builder webClientBuilder,
                         @Value("${app.integrations.jira.base-url}") String baseUrl,
                         @Value("${app.integrations.jira.api-user}") String apiUser,
                         @Value("${app.integrations.jira.api-token}") String apiToken) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(h -> h.setBasicAuth(apiUser, apiToken))
                .build();
    }

    /**
     * Асинхронно публикует комментарий к задаче в Jira.
     *
     * @param issueKey Ключ задачи (например, "PROJ-123").
     * @param comment  Текст комментария.
     * @return {@link Mono}, который завершается при успешной публикации.
     */
    public Mono<Void> postCommentToIssue(String issueKey, String comment) {
        log.info("Публикация комментария в задачу Jira: {}", issueKey);
        // API Jira для комментариев требует сложной структуры Atlassian Document Format
        Map<String, Object> body = Map.of(
                "body", Map.of(
                        "type", "doc",
                        "version", 1,
                        "content", List.of(
                                Map.of(
                                        "type", "paragraph",
                                        "content", List.of(
                                                Map.of("type", "text", "text", comment)
                                        )
                                )
                        )
                )
        );

        return webClient.post()
                .uri("/rest/api/3/issue/{issueKey}/comment", issueKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Комментарий успешно опубликован в задаче {}", issueKey))
                .doOnError(e -> log.error("Ошибка при публикации комментария в задачу {}: {}", issueKey, e.getMessage()));
    }
}
