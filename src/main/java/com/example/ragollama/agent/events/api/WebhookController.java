package com.example.ragollama.agent.events.api;

import com.example.ragollama.agent.events.EventProcessingService;
import com.example.ragollama.agent.events.security.WebhookSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер-шлюз для приема входящих веб-хуков от внешних систем.
 * <p>
 * Эта версия напрямую вызывает асинхронные методы {@link EventProcessingService},
 * устраняя зависимость от RabbitMQ. Контроллер по-прежнему немедленно возвращает
 * ответ `202 Accepted`, так как вся тяжелая работа выполняется в фоновом режиме
 * благодаря аннотации `@Async` на методах сервиса.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookSecurityService securityService;
    private final EventProcessingService eventProcessingService;

    @Value("${app.integrations.jira.webhook-secret-token}")
    private String jiraSecretToken;

    /**
     * Принимает веб-хуки от GitHub.
     *
     * @param signature Заголовок с подписью 'X-Hub-Signature-256'.
     * @param eventType Заголовок с типом события 'X-GitHub-Event'.
     * @param payload   Тело запроса.
     * @return {@link ResponseEntity} со статусом 202 (Accepted) или 401 (Unauthorized).
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
        log.info("Получен валидный веб-хук от GitHub, событие: '{}'. Запуск асинхронной обработки.", eventType);
        eventProcessingService.processGitHubEvent(eventType, payload);
        return ResponseEntity.accepted().build();
    }

    /**
     * Принимает веб-хуки от Jira.
     *
     * @param authToken Токен для простой проверки аутентичности.
     * @param payload   Тело запроса.
     * @return {@link ResponseEntity} со статусом 202 (Accepted) или 401 (Unauthorized).
     */
    @PostMapping("/jira")
    public ResponseEntity<Void> handleJiraWebhook(
            @RequestParam("token") String authToken,
            @RequestBody String payload) {
        if (!jiraSecretToken.equals(authToken)) {
            log.warn("Получен веб-хук от Jira с невалидным токеном.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Получен валидный веб-хук от Jira. Запуск асинхронной обработки.");
        eventProcessingService.processJiraEvent(payload);
        return ResponseEntity.accepted().build();
    }
}
