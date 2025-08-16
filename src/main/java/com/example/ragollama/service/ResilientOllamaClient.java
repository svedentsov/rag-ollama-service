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
 * Инкапсулирует логику вызовов к LLM, оборачивая их в паттерны
 * отказоустойчивости (Retry, Circuit Breaker, TimeLimiter) из библиотеки Resilience4j.
 * Предоставляет методы для асинхронного (возвращает CompletableFuture) и потокового
 * взаимодействия с моделью, корректно изолируя блокирующие операции.
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

    private CircuitBreaker circuitbreaker;
    private Retry retry;
    private TimeLimiter timeLimiter;

    /**
     * Инициализирует компоненты Resilience4j после создания бина.
     * Загружает конфигурации из application.yml по имени 'ollama'.
     */
    @PostConstruct
    public void init() {
        this.circuitbreaker = circuitBreakerRegistry.circuitBreaker(OLLAMA_CONFIG_NAME);
        this.retry = retryRegistry.retry(OLLAMA_CONFIG_NAME);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(OLLAMA_CONFIG_NAME);
    }

    /**
     * Асинхронно вызывает чат-модель для получения полного ответа.
     * <p>
     * Метод корректно обрабатывает блокирующий вызов Spring AI API,
     * перенося его выполнение в специализированный пул потоков,
     * чтобы не блокировать основной пул.
     * Вызов защищен политиками Resilience4j и обернут в таймер для сбора метрик.
     *
     * @param prompt Промпт для отправки в модель.
     * @return {@link CompletableFuture}, который будет завершен строковым ответом от LLM.
     */
    public CompletableFuture<String> callChat(Prompt prompt) {
        // ШАГ 1: Оборачиваем БЛОКИРУЮЩИЙ вызов `.call().content()` в `Mono.fromCallable`.
        // Это откладывает его выполнение до момента подписки.
        Mono<String> responseMono = Mono.fromCallable(() -> chatClient.prompt(prompt).call().content())
                // ШАГ 2: Переключаем выполнение блокирующей операции на специальный
                // пул потоков `boundedElastic`, предназначенный для I/O-задач.
                .subscribeOn(Schedulers.boundedElastic());
        Mono<String> resilientMono = applyResilienceToMono(responseMono);
        return metricService.recordTimer(
                "llm.requests",
                resilientMono::toFuture);
    }

    /**
     * Вызывает чат-модель для получения потокового ответа (SSE).
     * Поток защищен политиками Resilience4j и обернут в таймер для сбора метрик.
     *
     * @param prompt Промпт для отправки в модель.
     * @return {@link Flux}, который эмитит части (токены) ответа от LLM по мере их генерации.
     */
    public Flux<String> streamChat(Prompt prompt) {
        Flux<String> responseFlux = chatClient.prompt(prompt).stream().content();
        Flux<String> resilientFlux = applyResilienceToFlux(responseFlux);
        return metricService.recordTimer(
                "llm.requests.stream",
                () -> resilientFlux);
    }

    private <T> Flux<T> applyResilienceToFlux(Flux<T> publisher) {
        return publisher
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitbreaker))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter));
    }

    private <T> Mono<T> applyResilienceToMono(Mono<T> publisher) {
        return publisher
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitbreaker))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter));
    }
}
