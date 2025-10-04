package com.example.ragollama.shared.web;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Реактивный веб-фильтр для применения ограничений частоты запросов.
 * <p>
 * Эта реализация заменяет собой {@code HandlerInterceptor} из Spring MVC
 * и полностью интегрирована в неблокирующую цепочку обработки WebFlux.
 * Он перехватывает входящие запросы и проверяет наличие доступных "токенов".
 * <p>
 * Фильтр активируется свойством {@code app.rate-limiting.enabled=true}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Выполняется после RequestIdFilter
public class RateLimitFilter implements WebFilter {

    private final RateLimitingService rateLimitingService;

    /**
     * Основной метод фильтра, который применяет логику ограничения.
     *
     * @param exchange Объект, инкапсулирующий запрос и ответ.
     * @param chain    Цепочка фильтров.
     * @return {@link Mono}, сигнализирующий о завершении обработки.
     */
    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String endpoint = exchange.getRequest().getURI().getPath();
        Bucket bucket = rateLimitingService.resolveBucket(endpoint);

        // Если для эндпоинта не настроен лимит, пропускаем запрос
        if (bucket == null) {
            return chain.filter(exchange);
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Токен успешно потреблен, запрос разрешен
            exchange.getResponse().getHeaders().add("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return chain.filter(exchange);
        } else {
            // Токены закончились, запрос блокируется
            long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefillSeconds));
            log.warn("Превышен лимит запросов для эндпоинта {}", endpoint);
            // Завершаем цепочку, не передавая запрос дальше
            return exchange.getResponse().setComplete();
        }
    }
}
