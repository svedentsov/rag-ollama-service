package com.example.ragollama.qaagent.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для RabbitMQ.
 * <p>
 * Добавлены новые очереди и биндинги для поддержки
 * событийно-ориентированного конвейера индексации документов.
 */
@Configuration
@EnableRabbit
public class RabbitMqConfig {

    public static final String EVENTS_EXCHANGE = "events.exchange";
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";

    // Очереди для веб-хуков
    public static final String GITHUB_EVENTS_QUEUE = "github.events.queue";
    public static final String JIRA_EVENTS_QUEUE = "jira.events.queue";

    // Новые очереди и ключи для конвейера индексации
    public static final String JOB_BATCH_CLAIMED_QUEUE = "ingestion.batch.claimed.queue";
    public static final String DOCUMENT_PROCESSING_QUEUE = "ingestion.document.processing.queue";
    public static final String JOB_BATCH_CLAIMED_ROUTING_KEY = "ingestion.batch.claimed";
    public static final String DOCUMENT_PROCESSING_ROUTING_KEY = "ingestion.document.process";

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

    // --- Конфигурация очередей для веб-хуков ---

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
        return BindingBuilder.bind(jiraEventsQueue).to(exchange).with("jira.#");
    }

    // --- Конфигурация очередей для конвейера индексации ---

    @Bean
    public Queue jobBatchClaimedQueue() {
        return createDurableQueue(JOB_BATCH_CLAIMED_QUEUE);
    }

    @Bean
    public Queue documentProcessingQueue() {
        return createDurableQueue(DOCUMENT_PROCESSING_QUEUE);
    }

    @Bean
    public Binding jobBatchClaimedBinding(TopicExchange exchange, Queue jobBatchClaimedQueue) {
        return BindingBuilder.bind(jobBatchClaimedQueue).to(exchange).with(JOB_BATCH_CLAIMED_ROUTING_KEY);
    }

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
