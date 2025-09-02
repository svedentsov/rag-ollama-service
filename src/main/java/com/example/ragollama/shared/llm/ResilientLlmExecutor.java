package com.example.ragollama.shared.llm;

import com.example.ragollama.shared.metrics.MetricService;
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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Декоратор, который оборачивает вызовы к LLM в политики отказоустойчивости и метрики.
 * <p>
 * Этот класс реализует паттерн "Декоратор" и отвечает исключительно за
 * применение нефункциональных требований (надежность, наблюдаемость) к
 * асинхронным операциям, представленным в виде {@link Mono} или {@link Flux}.
 */
@Component
@RequiredArgsConstructor
public class ResilientLlmExecutor {

    private final MetricService metricService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    private static final String OLLAMA_CONFIG_NAME = "ollama";
    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private TimeLimiter timeLimiter;

    /**
     * Инициализирует компоненты Resilience4j после создания бина.
     */
    @PostConstruct
    public void init() {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(OLLAMA_CONFIG_NAME);
        this.retry = retryRegistry.retry(OLLAMA_CONFIG_NAME);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(OLLAMA_CONFIG_NAME);
    }

    /**
     * Выполняет не-потоковый вызов, обернутый в политики отказоустойчивости.
     *
     * @param monoSupplier {@link Supplier}, возвращающий "сырой" {@link Mono} с вызовом LLM.
     * @param <T>          Тип результата.
     * @return {@link Mono} с примененными политиками.
     */
    public <T> Mono<T> execute(Supplier<Mono<T>> monoSupplier) {
        return metricService.recordTimer("llm.requests", () ->
                Mono.defer(monoSupplier)
                        .transformDeferred(RetryOperator.of(retry))
                        .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        );
    }

    /**
     * Выполняет потоковый вызов, обернутый в политики отказоустойчивости.
     *
     * @param fluxSupplier {@link Supplier}, возвращающий "сырой" {@link Flux} с вызовом LLM.
     * @param <T>          Тип элементов в потоке.
     * @return {@link Flux} с примененными политиками.
     */
    public <T> Flux<T> executeStream(Supplier<Flux<T>> fluxSupplier) {
        return metricService.recordTimer("llm.requests.stream", () ->
                Flux.defer(fluxSupplier)
                        .transformDeferred(RetryOperator.of(retry))
                        .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        );
    }
}
