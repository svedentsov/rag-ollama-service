package com.example.ragollama.agent.accessibility.tools;

import com.example.ragollama.agent.accessibility.model.AccessibilityViolation;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Асинхронный, отказоустойчивый клиент для взаимодействия с внешним
 * микросервисом, выполняющим сканирование доступности.
 * <p>
 * Эталонная реализация, демонстрирующая интеграцию с внешним API
 * в реактивном стеке с использованием {@link WebClient} и Resilience4j.
 * Все вызовы защищены паттернами Retry, Circuit Breaker и Timeout.
 */
@Slf4j
@Service
public class ExternalAccessibilityScannerClient {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    private static final String RESILIENCE_CONFIG_NAME = "accessibilityScanner";

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param webClientBuilder       Глобальный строитель WebClient.
     * @param scannerBaseUrl         URL внешнего сервиса из application.yml.
     * @param circuitBreakerRegistry Реестр Circuit Breaker'ов.
     * @param retryRegistry          Реестр Retry.
     * @param timeLimiterRegistry    Реестр Time Limiter'ов.
     */
    public ExternalAccessibilityScannerClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.integrations.accessibility-scanner.base-url:http://localhost:8081}") String scannerBaseUrl,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.webClient = webClientBuilder.baseUrl(scannerBaseUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_CONFIG_NAME);
        this.retry = retryRegistry.retry(RESILIENCE_CONFIG_NAME);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(RESILIENCE_CONFIG_NAME);
    }

    /**
     * Асинхронно отправляет HTML-контент на анализ во внешний сервис.
     * <p>
     * Вызов оборачивается в операторы Resilience4j для обеспечения
     * отказоустойчивости. В случае ошибки после всех попыток или при
     * разомкнутом Circuit Breaker, возвращается пустой список нарушений,
     * что позволяет конвейеру продолжить работу в деградированном режиме.
     *
     * @param htmlContent HTML-код для анализа.
     * @return {@link Mono}, который по завершении будет содержать список
     * обнаруженных нарушений или пустой список в случае сбоя.
     */
    public Mono<List<AccessibilityViolation>> scan(String htmlContent) {
        log.info("Отправка запроса на асинхронное сканирование доступности...");

        Mono<List<AccessibilityViolation>> requestMono = webClient.post()
                .uri("/api/v1/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("htmlContent", htmlContent))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<AccessibilityViolation>>() {
                });

        return requestMono
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doOnError(e -> log.error("Ошибка при вызове сканера доступности после всех попыток.", e))
                .onErrorReturn(Collections.emptyList()); // Fallback: возвращаем пустой список
    }
}
