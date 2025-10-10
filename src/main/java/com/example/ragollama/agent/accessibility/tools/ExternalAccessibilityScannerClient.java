package com.example.ragollama.agent.accessibility.tools;

import com.example.ragollama.agent.accessibility.model.AccessibilityViolation;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Асинхронный клиент для взаимодействия с внешним микросервисом,
 * выполняющим сканирование доступности.
 * <p>
 * Этот класс инкапсулирует всю сетевую логику и защищен механизмами
 * отказоустойчивости (Retry, Circuit Breaker, Timeout) от Resilience4j.
 * ВАЖНО: Текущая реализация является **mock-заглушкой** для демонстрации
 * архитектурного паттерна.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalAccessibilityScannerClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.integrations.accessibility-scanner.base-url:http://localhost:8081}")
    private String scannerBaseUrl;

    /**
     * Асинхронно отправляет HTML-контент на анализ во внешний сервис.
     *
     * @param htmlContent HTML-код для анализа.
     * @return {@link Mono}, который по завершении будет содержать список
     * обнаруженных нарушений.
     */
    @CircuitBreaker(name = "accessibilityScanner")
    @Retry(name = "accessibilityScanner")
    @TimeLimiter(name = "accessibilityScanner")
    public Mono<List<AccessibilityViolation>> scan(String htmlContent) {
        log.info("Отправка запроса на асинхронное сканирование доступности...");

        // ======================= MOCK IMPLEMENTATION =======================
        // В реальном приложении здесь будет вызов WebClient, как показано ниже.
        // Для демонстрации мы возвращаем предопределенный набор нарушений.
        return Mono.fromCallable(() -> {
            if (htmlContent != null && htmlContent.contains("<img src=")) {
                if (!htmlContent.matches(".*<img[^>]+alt=.*")) {
                    return List.of(
                            new AccessibilityViolation(
                                    "image-alt", "critical", "Images must have alternate text",
                                    "https://dequeuniversity.com/rules/axe/4.4/image-alt", List.of("img[src=\"logo.png\"]")
                            )
                    );
                }
            }
            return List.of();
        });
    }
}
