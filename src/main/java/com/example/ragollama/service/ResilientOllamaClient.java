package com.example.ragollama.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Отказоустойчивый клиент для взаимодействия с Ollama API.
 * <p>
 * Этот компонент является "оберткой" над стандартным {@link ChatClient} из Spring AI,
 * добавляя слои отказоустойчивости с помощью Resilience4j (Circuit Breaker, Retry, TimeLimiter).
 * Его единственная ответственность — обеспечить надежное взаимодействие с LLM.
 */
@Component
@RequiredArgsConstructor
public class ResilientOllamaClient {

    private final ChatClient chatClient;
    private final MetricService metricService;

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    @Qualifier("resilience4jScheduler")
    private final ScheduledExecutorService scheduler;

    private static final String OLLAMA_CONFIG_NAME = "ollama";

    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private TimeLimiter timeLimiter;

    @PostConstruct
    public void init() {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(OLLAMA_CONFIG_NAME);
        this.retry = retryRegistry.retry(OLLAMA_CONFIG_NAME);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(OLLAMA_CONFIG_NAME);
    }

    /**
     * Выполняет не-стриминговый вызов к LLM с применением политик отказоустойчивости.
     *
     * @param prompt Промпт для LLM.
     * @return CompletableFuture со строковым ответом от модели.
     */
    public CompletableFuture<String> callChat(Prompt prompt) {
        Supplier<CompletableFuture<String>> supplier = () -> CompletableFuture.supplyAsync(
                () -> metricService.recordTimer("llm.requests",
                        () -> chatClient.prompt(prompt).call().content()));
        return executeWithResilience(supplier);
    }

    /**
     * Выполняет стриминговый вызов к LLM с применением политик отказоустойчивости в реактивном стиле.
     *
     * @param prompt Промпт для LLM.
     * @return {@link Flux}, эммитящий токены ответа по мере их генерации и защищенный
     *         механизмами Retry, CircuitBreaker и TimeLimiter.
     */
    public Flux<String> streamChat(Prompt prompt) {
        return chatClient.prompt(prompt)
                .stream()
                .content()
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter));
    }

    /**
     * Приватный helper-метод для "оборачивания" асинхронной операции всеми
     * настроенными паттернами отказоустойчивости.
     */
    private <T> CompletableFuture<T> executeWithResilience(Supplier<CompletableFuture<T>> supplier) {
        Supplier<CompletionStage<T>> timeLimitedSupplier = () ->
                timeLimiter.executeCompletionStage(this.scheduler, supplier);

        Supplier<CompletionStage<T>> circuitSupplier = () ->
                circuitBreaker.executeCompletionStage(timeLimitedSupplier);

        CompletionStage<T> resultStage = retry.executeCompletionStage(this.scheduler, circuitSupplier);

        return resultStage.toCompletableFuture();
    }
}
