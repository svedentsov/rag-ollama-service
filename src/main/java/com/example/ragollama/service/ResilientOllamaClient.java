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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

/**
 * Отказоустойчивый клиент для взаимодействия с Ollama API.
 */
@Component
@RequiredArgsConstructor
public class ResilientOllamaClient {

    private final ChatClient chatClient;
    private final MetricService metricService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

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
     * Неблокирующий (реактивный) вызов, возвращающий CompletableFuture<String>.
     * Оборачиваем блокирующий call().content() в Mono.fromCallable и выполняем его на boundedElastic.
     */
    public CompletableFuture<String> callChat(Prompt prompt) {
        Mono<String> mono = Mono.defer(() ->
                Mono.fromCallable(() -> chatClient.prompt(prompt).call().content())
                        .subscribeOn(Schedulers.boundedElastic()));
        Mono<String> resilient = applyResilience(mono);
        return metricService.recordTimer("llm.requests", () -> resilient.toFuture());
    }

    /**
     * Выполняет стриминговый вызов к LLM с применением политик отказоустойчивости в реактивном стиле.
     *
     * @param prompt Промпт для LLM.
     * @return {@link Flux}, эммитящий токены ответа по мере их генерации и защищенный
     * механизмами Retry, CircuitBreaker и TimeLimiter.
     */
    public Flux<String> streamChat(Prompt prompt) {
        Flux<String> flux = Flux.defer(() -> chatClient.prompt(prompt).stream().content());
        Flux<String> resilientFlux = applyResilience(flux);
        return metricService.recordTimer("llm.requests.stream", () -> resilientFlux);
    }

    /**
     * Применяет операторы Resilience4j к Flux.
     */
    private <T> Flux<T> applyResilience(Flux<T> publisher) {
        return publisher
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .onErrorMap(ex -> {
                    // можно логировать/замерять ошибки здесь
                    return ex;
                });
    }

    /**
     * Применяет операторы Resilience4j к Mono.
     */
    private <T> Mono<T> applyResilience(Mono<T> publisher) {
        return publisher
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter));
    }
}
