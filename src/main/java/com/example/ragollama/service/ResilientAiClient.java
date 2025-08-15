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
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ResilientAiClient {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final MetricService metricService;

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    @Qualifier("resilience4jScheduler")
    private final ScheduledExecutorService scheduler;

    private static final String RESILIENCE_CONFIG_NAME = "ollamaApi";
    /**
     * Имя новой, более "терпеливой" конфигурации для стриминговых вызовов чата.
     */
    private static final String CHAT_RESILIENCE_CONFIG_NAME = "ollamaChatApi";

    private CircuitBreaker ollamaCircuitBreaker;
    private Retry ollamaRetry;
    private TimeLimiter ollamaTimeLimiter;

    // Новые поля для конфигурации чата
    private CircuitBreaker ollamaChatCircuitBreaker;
    private Retry ollamaChatRetry;
    private TimeLimiter ollamaChatTimeLimiter;

    @PostConstruct
    public void init() {
        // Инициализация стандартной конфигурации для быстрых операций
        this.ollamaCircuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_CONFIG_NAME);
        this.ollamaRetry = retryRegistry.retry(RESILIENCE_CONFIG_NAME);
        this.ollamaTimeLimiter = timeLimiterRegistry.timeLimiter(RESILIENCE_CONFIG_NAME);

        // Инициализация новой конфигурации для долгих операций генерации
        this.ollamaChatCircuitBreaker = circuitBreakerRegistry.circuitBreaker(CHAT_RESILIENCE_CONFIG_NAME);
        this.ollamaChatRetry = retryRegistry.retry(CHAT_RESILIENCE_CONFIG_NAME);
        this.ollamaChatTimeLimiter = timeLimiterRegistry.timeLimiter(CHAT_RESILIENCE_CONFIG_NAME);
    }

    public CompletableFuture<String> callChat(Prompt prompt) {
        Supplier<CompletableFuture<String>> supplier = () -> CompletableFuture.supplyAsync(
                () -> metricService.recordTimer("llm.requests",
                        () -> chatClient.prompt(prompt).call().content()
                )
        );
        // Для не-стримингового чата используем более долгий таймаут
        return executeWithResilience(supplier, ollamaChatCircuitBreaker, ollamaChatRetry, ollamaChatTimeLimiter);
    }

    public Flux<String> streamChat(Prompt prompt) {
        return metricService.recordTimer("llm.stream.requests",
                        () -> chatClient.prompt(prompt).stream().content()
                )
                // Используем новые, выделенные операторы для стриминга с увеличенным таймаутом
                .transform(CircuitBreakerOperator.of(this.ollamaChatCircuitBreaker))
                .transform(RetryOperator.of(this.ollamaChatRetry))
                .transform(TimeLimiterOperator.of(this.ollamaChatTimeLimiter));
    }

    public CompletableFuture<Object> similaritySearch(SearchRequest searchRequest) {
        Supplier<CompletableFuture<Object>> supplier = () -> CompletableFuture.supplyAsync(
                () -> metricService.recordTimer("rag.retrieval",
                        () -> vectorStore.similaritySearch(searchRequest)
                )
        );
        // Для быстрого поиска по векторам используем стандартную, быструю конфигурацию
        return executeWithResilience(supplier, ollamaCircuitBreaker, ollamaRetry, ollamaTimeLimiter);
    }

    /**
     * Приватный helper-метод для "оборачивания" асинхронной операции всеми
     * настроенными паттернами отказоустойчивости.
     *
     * @param supplier       Функция, возвращающая {@link CompletableFuture} с результатом.
     * @param circuitBreaker Конкретный Circuit Breaker для использования.
     * @param retry          Конкретный Retry для использования.
     * @param timeLimiter    Конкретный TimeLimiter для использования.
     * @param <T>            Тип результата.
     * @return {@link CompletableFuture}, защищенный паттернами Resilience4j.
     */
    private <T> CompletableFuture<T> executeWithResilience(Supplier<CompletableFuture<T>> supplier,
                                                           CircuitBreaker circuitBreaker,
                                                           Retry retry,
                                                           TimeLimiter timeLimiter) {
        Supplier<CompletionStage<T>> timeLimitedSupplier = () ->
                timeLimiter.executeCompletionStage(this.scheduler, supplier);
        Supplier<CompletionStage<T>> circuitSupplier = () ->
                circuitBreaker.executeCompletionStage(timeLimitedSupplier);
        CompletionStage<T> resultStage = retry.executeCompletionStage(this.scheduler, circuitSupplier);
        return resultStage.toCompletableFuture();
    }
}
