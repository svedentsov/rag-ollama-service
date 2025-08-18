package com.example.ragollama.shared.config;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.metrics.MetricService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для централизованного создания и настройки бинов, связанных с AI.
 * Эта версия класса полностью полагается на автоконфигурацию Spring AI для
 * создания низкоуровневых компонентов (таких как OllamaChatModel). Мы лишь
 * внедряем готовый {@link ChatClient.Builder} для построения нашего
 * отказоустойчивого фасада {@link LlmClient}.
 * Такой подход значительно упрощает код, повышает его надежность и соответствие "the Spring Boot way".
 */
@Configuration
public class AiConfig {

    /**
     * Создает и предоставляет единственный, отказоустойчивый бин {@link LlmClient}.
     * Этот метод получает от Spring уже полностью сконфигурированный
     * {@link ChatClient.Builder} и использует его для создания "сырого" клиента,
     * который затем оборачивается в наш отказоустойчивый фасад.
     *
     * @param chatClientBuilder      Автоматически сконфигурированный строитель ChatClient.
     * @param metricService          Сервис для сбора метрик.
     * @param circuitBreakerRegistry Реестр Circuit Breaker'ов.
     * @param retryRegistry          Реестр политик Retry.
     * @param timeLimiterRegistry    Реестр TimeLimiter'ов.
     * @return Единственный экземпляр {@link LlmClient} для использования в приложении.
     */
    @Bean
    public LlmClient llmClient(
            ChatClient.Builder chatClientBuilder,
            MetricService metricService,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry
    ) {
        ChatClient internalChatClient = chatClientBuilder.build();
        return new LlmClient(
                internalChatClient,
                metricService,
                circuitBreakerRegistry,
                retryRegistry,
                timeLimiterRegistry
        );
    }

    /**
     * Создает бин {@link TokenTextSplitter} для разбиения текста на чанки.
     * Этот компонент является утилитарным и может быть публичным.
     *
     * @return Экземпляр {@link TokenTextSplitter}.
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }
}
