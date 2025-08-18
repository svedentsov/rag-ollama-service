package com.example.ragollama.shared.llm;

import com.example.ragollama.shared.config.AiConfig;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

/**
 * Отказоустойчивый клиент, являющийся единственной точкой входа
 * для взаимодействия с LLM в приложении.
 * Этот компонент инкапсулирует применение паттернов отказоустойчивости
 * (Retry, Circuit Breaker, TimeLimiter), гарантируя, что все обращения
 * к внешнему сервису (Ollama) защищены от сбоев. Он полностью скрывает
 * "сырой" {@link ChatClient} от остального приложения, следуя принципу "Pit of Success".
 * * Важно: этот класс НЕ является Spring-компонентом (@Component),
 * * а создается и настраивается исключительно через @Bean-метод в {@link AiConfig}.
 */
public class LlmClient {

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
     * Конструктор для внедрения зависимостей.
     *
     * @param chatClient             Экземпляр "сырого" ChatClient, предоставляемый конфигурацией.
     * @param metricService          Сервис для сбора метрик.
     * @param circuitBreakerRegistry Реестр для управления Circuit Breaker'ами.
     * @param retryRegistry          Реестр для управления политиками Retry.
     * @param timeLimiterRegistry    Реестр для управления TimeLimiter'ами.
     */
    public LlmClient(
            ChatClient chatClient,
            MetricService metricService,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.chatClient = chatClient;
        this.metricService = metricService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    /**
     * Инициализирует компоненты Resilience4j после создания бина.
     * Этот метод гарантирует, что экземпляры {@link CircuitBreaker}, {@link Retry}
     * и {@link TimeLimiter} будут получены из соответствующих реестров
     * до первого вызова клиента.
     */
    @PostConstruct
    public void init() {
        this.circuitbreaker = circuitBreakerRegistry.circuitBreaker(OLLAMA_CONFIG_NAME);
        this.retry = retryRegistry.retry(OLLAMA_CONFIG_NAME);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(OLLAMA_CONFIG_NAME);
    }

    /**
     * Асинхронно вызывает чат-модель для получения полного, не-потокового ответа.
     * Оборачивает блокирующий вызов Spring AI в {@code Mono.fromCallable} и выполняет
     * его в специализированном пуле потоков {@code Schedulers.boundedElastic()},
     * предотвращая блокировку основного event-loop и обеспечивая масштабируемость.
     *
     * @param prompt Промпт для отправки в модель.
     * @return {@link CompletableFuture}, который будет завершен строковым ответом от LLM.
     */
    public CompletableFuture<String> callChat(Prompt prompt) {
        Mono<String> responseMono = Mono.fromCallable(() -> chatClient.prompt(prompt).call().content())
                .subscribeOn(Schedulers.boundedElastic());
        Mono<String> resilientMono = applyResilienceToMono(responseMono);
        return metricService.recordTimer("llm.requests", resilientMono::toFuture);
    }

    /**
     * Вызывает чат-модель для получения ответа в виде непрерывного потока токенов.
     * Этот метод является полностью неблокирующим и идеально подходит для
     * реализации потоковой передачи (SSE) на клиент.
     *
     * @param prompt Промпт для отправки в модель.
     * @return {@link Flux}, который асинхронно эмитит части ответа от LLM.
     */
    public Flux<String> streamChat(Prompt prompt) {
        Flux<String> responseFlux = chatClient.prompt(prompt).stream().content();
        Flux<String> resilientFlux = applyResilienceToFlux(responseFlux);
        return metricService.recordTimer("llm.requests.stream", () -> resilientFlux);
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
