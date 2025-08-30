package com.example.ragollama.agent.events;

import com.example.ragollama.agent.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Сервис-слушатель для асинхронной обработки сообщений из RabbitMQ.
 * <p>
 * Этот класс является "точкой входа" для событий. Его единственная
 * задача - принять сообщение из очереди и немедленно делегировать
 * его обработку специализированному сервису {@link EventProcessingService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumerService {

    private final EventProcessingService eventProcessingService;

    /**
     * Слушает очередь событий от GitHub и делегирует их обработку.
     *
     * @param payload    Сырой JSON payload.
     * @param routingKey Ключ маршрутизации, содержащий тип события.
     */
    @RabbitListener(queues = RabbitMqConfig.GITHUB_EVENTS_QUEUE)
    public void consumeGitHubEvent(String payload, @org.springframework.messaging.handler.annotation.Header("amqp_receivedRoutingKey") String routingKey) {
        String eventType = routingKey.replace("github.", "");
        log.info("Получено событие '{}' из очереди GitHub. Делегирование в EventProcessingService.", eventType);

        switch (eventType) {
            case "pull_request" -> eventProcessingService.processGitHubPullRequestEvent(payload);
            case "push" -> eventProcessingService.processGitHubPushEvent(payload);
            default -> log.trace("Пропускаем необрабатываемое событие GitHub: {}", eventType);
        }
    }

    /**
     * Слушает очередь событий от Jira и делегирует их обработку.
     *
     * @param payload Сырой JSON payload.
     */
    @RabbitListener(queues = RabbitMqConfig.JIRA_EVENTS_QUEUE)
    public void consumeJiraEvent(String payload) {
        log.info("Получено событие из очереди Jira. Делегирование в EventProcessingService.");
        eventProcessingService.processJiraEvent(payload);
    }
}
