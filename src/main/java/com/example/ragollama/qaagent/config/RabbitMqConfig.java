package com.example.ragollama.qaagent.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    public static final String EVENTS_EXCHANGE = "events.exchange";
    public static final String GITHUB_EVENTS_QUEUE = "github.events.queue";
    public static final String JIRA_EVENTS_QUEUE = "jira.events.queue";
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE);
    }

    @Bean
    public Queue githubEventsQueue() {
        return QueueBuilder.durable(GITHUB_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", "") // Роутинг в DLQ по умолчанию
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue jiraEventsQueue() {
        return QueueBuilder.durable(JIRA_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE);
    }

    @Bean
    public Binding githubBinding(TopicExchange exchange, Queue githubEventsQueue) {
        return BindingBuilder.bind(githubEventsQueue).to(exchange).with("github.#");
    }

    @Bean
    public Binding jiraBinding(TopicExchange exchange, Queue jiraEventsQueue) {
        return BindingBuilder.bind(jiraEventsQueue).to(exchange).with("jira.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
