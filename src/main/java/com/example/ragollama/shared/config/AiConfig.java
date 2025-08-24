package com.example.ragollama.shared.config;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.LlmRouterService;
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
 * Эта версия обновлена для создания `LlmClient` с новым `LlmRouterService`.
 */
@Configuration
public class AiConfig {

    /**
     * Создает и предоставляет единственный, отказоустойчивый и интеллектуальный бин {@link LlmClient}.
     *
     * @param chatClientBuilder      Автоматически сконфигурированный строитель ChatClient.
     * @param metricService          Сервис для сбора метрик.
     * @param llmRouterService       Сервис для выбора модели.
     * @param circuitBreakerRegistry Реестр Circuit Breaker'ов.
     * @param retryRegistry          Реестр политик Retry.
     * @param timeLimiterRegistry    Реестр TimeLimiter'ов.
     * @return Единственный экземпляр {@link LlmClient} для использования в приложении.
     */
    @Bean
    public LlmClient llmClient(
            ChatClient.Builder chatClientBuilder,
            MetricService metricService,
            LlmRouterService llmRouterService,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry
    ) {
        return new LlmClient(
                chatClientBuilder,
                metricService,
                llmRouterService,
                circuitBreakerRegistry,
                retryRegistry,
                timeLimiterRegistry
        );
    }
}
