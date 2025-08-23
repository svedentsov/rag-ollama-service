package com.example.ragollama.shared.config;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.metrics.MetricService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для централизованного создания и настройки бинов, связанных с AI.
 * <p>
 * Эта финальная версия полностью отказывается от стандартного TokenTextSplitter
 * в пользу нашего кастомного, более мощного TextSplitterService.
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
}
