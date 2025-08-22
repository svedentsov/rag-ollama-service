package com.example.ragollama.qaagent.events;

import com.example.ragollama.qaagent.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Сервис-слушатель для асинхронной обработки сообщений из RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumerService {

    private final EventProcessingService eventProcessingService;

    @RabbitListener(queues = RabbitMqConfig.GITHUB_EVENTS_QUEUE)
    public void consumeGitHubEvent(String payload) {
        log.info("Получено событие из очереди GitHub.");
        eventProcessingService.processGitHubPullRequestEvent(payload);
    }

    @RabbitListener(queues = RabbitMqConfig.JIRA_EVENTS_QUEUE)
    public void consumeJiraEvent(String payload) {
        log.info("Получено событие из очереди Jira.");
        eventProcessingService.processJiraIssueEvent(payload);
    }
}
