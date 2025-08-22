package com.example.ragollama.qaagent.web;

import com.example.ragollama.qaagent.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер-шлюз для приема входящих веб-хуков от внешних систем.
 * <p>
 * Этот контроллер является единой точкой входа для всех асинхронных событий.
 * Его основная задача — выполнить быструю проверку аутентичности,
 * опубликовать сырой payload события в соответствующую очередь RabbitMQ
 * и немедленно вернуть ответ `202 Accepted`. Вся дальнейшая обработка
 * происходит асинхронно в отдельных сервисах-слушателях.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookSecurityService securityService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.integrations.jira.webhook-secret-token}")
    private String jiraSecretToken;

    /**
     * Принимает веб-хуки от GitHub.
     * <p>
     * Проверяет HMAC-подпись, после чего публикует событие в RabbitMQ с
     * роутинг-ключом, основанным на типе события (например, "github.pull_request").
     *
     * @param signature Заголовок с подписью 'X-Hub-Signature-256'.
     * @param eventType Заголовок с типом события 'X-GitHub-Event'.
     * @param payload   Тело запроса.
     * @return {@link ResponseEntity} со статусом 202 (Accepted) в случае успеха
     * или 401 (Unauthorized) при невалидной подписи.
     */
    @PostMapping("/github")
    public ResponseEntity<Void> handleGitHubWebhook(
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestBody String payload) {

        if (!securityService.isValidGitHubSignature(payload, signature)) {
            log.warn("Получен веб-хук от GitHub с невалидной подписью.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Получен валидный веб-хук от GitHub, событие: '{}'. Публикация в очередь.", eventType);
        String routingKey = "github." + eventType;
        rabbitTemplate.convertAndSend(RabbitMqConfig.EVENTS_EXCHANGE, routingKey, payload);

        return ResponseEntity.accepted().build();
    }

    /**
     * Принимает веб-хуки от Jira.
     * <p>
     * Проверяет статический токен аутентификации, после чего публикует
     * событие в RabbitMQ с роутинг-ключом "jira.issue_created".
     *
     * @param authToken Токен для простой проверки аутентичности, передаваемый как query-параметр.
     * @param payload   Тело запроса.
     * @return {@link ResponseEntity} со статусом 202 (Accepted) в случае успеха
     * или 401 (Unauthorized) при невалидном токене.
     */
    @PostMapping("/jira")
    public ResponseEntity<Void> handleJiraWebhook(
            @RequestParam("token") String authToken,
            @RequestBody String payload) {

        if (!jiraSecretToken.equals(authToken)) {
            log.warn("Получен веб-хук от Jira с невалидным токеном.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Получен валидный веб-хук от Jira. Публикация в очередь.");
        rabbitTemplate.convertAndSend(RabbitMqConfig.EVENTS_EXCHANGE, "jira.issue_created", payload);

        return ResponseEntity.accepted().build();
    }
}
