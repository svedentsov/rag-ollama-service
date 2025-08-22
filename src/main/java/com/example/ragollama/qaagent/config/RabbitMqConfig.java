package com.example.ragollama.qaagent.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для RabbitMQ.
 * <p>
 * В этой версии архитектура конвейера индексации упрощена.
 * Удалена очередь `ingestion.batch.claimed.queue` и соответствующий
 * биндинг, так как теперь используется единая очередь для пакетной обработки.
 */
@Configuration
@EnableRabbit
public class RabbitMqConfig {

    public static final String EVENTS_EXCHANGE = "events.exchange";
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";

    // Очереди для веб-хуков
    public static final String GITHUB_EVENTS_QUEUE = "github.events.queue";
    public static final String JIRA_EVENTS_QUEUE = "jira.events.queue";

    // Упрощенная конфигурация для конвейера индексации
    public static final String DOCUMENT_PROCESSING_QUEUE = "ingestion.document.processing.queue";
    public static final String DOCUMENT_PROCESSING_ROUTING_KEY = "ingestion.document.process.batch";

    /**
     * Создает обменник типа "topic" для маршрутизации событий.
     *
     * @return Экземпляр {@link TopicExchange}.
     */
    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE);
    }

    /**
     * Создает очередь для "мертвых" сообщений.
     *
     * @return Экземпляр {@link Queue}.
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE);
    }

    @Bean
    public Queue githubEventsQueue() {
        return createDurableQueue(GITHUB_EVENTS_QUEUE);
    }

    @Bean
    public Queue jiraEventsQueue() {
        return createDurableQueue(JIRA_EVENTS_QUEUE);
    }

    @Bean
    public Binding githubBinding(TopicExchange exchange, Queue githubEventsQueue) {
        return BindingBuilder.bind(githubEventsQueue).to(exchange).with("github.#");
    }

    @Bean
    public Binding jiraBinding(TopicExchange exchange, Queue jiraEventsQueue) {
        return BindingBuilder.bind(jiraEventsQueue).to(exchange).with("jira.issue_created");
    }

    /**
     * Создает единую очередь для обработки пакетов документов.
     *
     * @return Экземпляр {@link Queue}.
     */
    @Bean
    public Queue documentProcessingQueue() {
        return createDurableQueue(DOCUMENT_PROCESSING_QUEUE);
    }

    /**
     * Связывает очередь обработки документов с обменником по ключу маршрутизации.
     *
     * @param exchange                Обменник.
     * @param documentProcessingQueue Очередь для обработки.
     * @return Объект {@link Binding}.
     */
    @Bean
    public Binding documentProcessingBinding(TopicExchange exchange, Queue documentProcessingQueue) {
        return BindingBuilder.bind(documentProcessingQueue).to(exchange).with(DOCUMENT_PROCESSING_ROUTING_KEY);
    }

    /**
     * Настраивает конвертер для автоматической сериализации/десериализации сообщений в JSON.
     *
     * @return Экземпляр {@link MessageConverter}.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Вспомогательный метод для создания отказоустойчивой, долговечной очереди с DLQ.
     *
     * @param queueName Имя создаваемой очереди.
     * @return Сконфигурированный экземпляр {@link Queue}.
     */
    private Queue createDurableQueue(String queueName) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                .build();
    }
}
