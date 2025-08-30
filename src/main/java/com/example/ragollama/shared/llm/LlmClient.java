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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

/**
 * Отказоустойчивый клиент, являющийся единственной точкой входа
 * для взаимодействия с LLM в приложении.
 * <p>
 * Эта версия интегрирована с {@link LlmRouterService}. Она делегирует
 * выбор конкретной модели роутеру, а сама фокусируется на применении
 */
public class LlmClient {

    private final ChatClient chatClient;
    private final MetricService metricService;
    private final LlmRouterService llmRouterService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    private static final String OLLAMA_CONFIG_NAME = "ollama";

    private CircuitBreaker circuitbreaker;
    private Retry retry;
    private TimeLimiter timeLimiter;

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param chatClientBuilder      Стандартный строитель ChatClient от Spring AI.
     * @param metricService          Сервис для сбора метрик.
     * @param llmRouterService       Сервис для выбора модели.
     * @param circuitBreakerRegistry Реестр Circuit Breaker'ов.
     * @param retryRegistry          Реестр Retry.
     * @param timeLimiterRegistry    Реестр Time Limiter'ов.
     */
    public LlmClient(
            ChatClient.Builder chatClientBuilder,
            MetricService metricService,
            LlmRouterService llmRouterService,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.chatClient = chatClientBuilder.build();
        this.metricService = metricService;
        this.llmRouterService = llmRouterService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    /**
     * Инициализирует компоненты Resilience4j после создания бина.
     * Загружает именованные конфигурации из реестров для дальнейшего использования.
     */
    @PostConstruct
    public void init() {
        this.circuitbreaker = circuitBreakerRegistry.circuitBreaker(OLLAMA_CONFIG_NAME);
        this.retry = retryRegistry.retry(OLLAMA_CONFIG_NAME);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(OLLAMA_CONFIG_NAME);
    }

    /**
     * Асинхронно вызывает чат-модель для получения полного, не-потокового ответа.
     * <p>
     * Вызов выполняется на отдельном пуле потоков (`boundedElastic`) для
     * предотвращения блокировки основного потока.
     *
     * @param prompt     Промпт для отправки в модель.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link CompletableFuture}, который будет завершен строковым ответом от LLM.
     */
    public CompletableFuture<String> callChat(Prompt prompt, ModelCapability capability) {
        Mono<String> responseMono = Mono.fromCallable(() -> {
                    String modelName = llmRouterService.getModelFor(capability);
                    OllamaOptions options = OllamaOptions.builder()
                            .model(modelName)
                            .build();
                    return chatClient.prompt(prompt)
                            .options(options)
                            .call()
                            .content();
                })
                .subscribeOn(Schedulers.boundedElastic());
        Mono<String> resilientMono = applyResilienceToMono(responseMono);
        return metricService.recordTimer("llm.requests", resilientMono::toFuture);
    }

    /**
     * Вызывает чат-модель для получения ответа в виде непрерывного потока токенов.
     *
     * @param prompt     Промпт для отправки в модель.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link Flux}, который асинхронно эмитит части ответа от LLM.
     */
    public Flux<String> streamChat(Prompt prompt, ModelCapability capability) {
        Flux<String> responseFlux = Flux.defer(() -> {
            String modelName = llmRouterService.getModelFor(capability);
            OllamaOptions options = OllamaOptions.builder()
                    .model(modelName)
                    .build();
            return chatClient.prompt(prompt)
                    .options(options)
                    .stream()
                    .content();
        });
        Flux<String> resilientFlux = applyResilienceToFlux(responseFlux);
        return metricService.recordTimer("llm.requests.stream", () -> resilientFlux);
    }

    /**
     * Применяет политики отказоустойчивости к потоковому (Flux) вызову.
     *
     * @param publisher Исходный {@link Flux}.
     * @param <T>       Тип элементов в потоке.
     * @return {@link Flux}, обернутый в механизмы Resilience4j.
     */
    private <T> Flux<T> applyResilienceToFlux(Flux<T> publisher) {
        return publisher
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitbreaker))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter));
    }

    /**
     * Применяет политики отказоустойчивости к асинхронному (Mono) вызову.
     *
     * @param publisher Исходный {@link Mono}.
     * @param <T>       Тип результата.
     * @return {@link Mono}, обернутый в механизмы Resilience4j.
     */
    private <T> Mono<T> applyResilienceToMono(Mono<T> publisher) {
        return publisher
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitbreaker))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter));
    }
}
