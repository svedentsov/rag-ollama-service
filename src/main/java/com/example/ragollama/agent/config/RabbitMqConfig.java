package com.example.ragollama.agent.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для RabbitMQ.
 * <p>
 * Эта финальная версия содержит только конфигурацию, необходимую
 * для обработки асинхронных событий от внешних систем (веб-хуков),
 * так как конвейер индексации был переведен на прямой @Async вызов.
 */
@Configuration
@EnableRabbit
public class RabbitMqConfig {

    public static final String EVENTS_EXCHANGE = "events.exchange";
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";

    // Очереди для веб-хуков
    public static final String GITHUB_EVENTS_QUEUE = "github.events.queue";
    public static final String JIRA_EVENTS_QUEUE = "jira.events.queue";

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

    /**
     * Создает очередь для событий от GitHub.
     *
     * @return Отказоустойчивая очередь с DLQ.
     */
    @Bean
    public Queue githubEventsQueue() {
        return createDurableQueue(GITHUB_EVENTS_QUEUE);
    }

    /**
     * Создает очередь для событий от Jira.
     *
     * @return Отказоустойчивая очередь с DLQ.
     */
    @Bean
    public Queue jiraEventsQueue() {
        return createDurableQueue(JIRA_EVENTS_QUEUE);
    }

    /**
     * Связывает очередь GitHub с обменником по ключу "github.#".
     */
    @Bean
    public Binding githubBinding(TopicExchange exchange, Queue githubEventsQueue) {
        return BindingBuilder.bind(githubEventsQueue).to(exchange).with("github.#");
    }

    /**
     * Связывает очередь Jira с обменником по ключу "jira.issue_created".
     */
    @Bean
    public Binding jiraBinding(TopicExchange exchange, Queue jiraEventsQueue) {
        return BindingBuilder.bind(jiraEventsQueue).to(exchange).with("jira.issue_created");
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
