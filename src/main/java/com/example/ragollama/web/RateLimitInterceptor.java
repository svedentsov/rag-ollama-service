package com.example.ragollama.web;

import com.example.ragollama.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC интерсептор для применения ограничений частоты запросов.
 * <p>
 * Перехватывает входящие запросы перед тем, как они достигнут контроллера,
 * и проверяет, есть ли доступные "токены" в соответствующем "ведре" (bucket).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String endpoint = request.getRequestURI();
        Bucket bucket = rateLimitingService.resolveBucket(endpoint);

        // Если для эндпоинта не настроен лимит, пропускаем запрос
        if (bucket == null) {
            return true;
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Токен успешно потреблен, запрос разрешен
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            // Токены закончились, запрос блокируется
            long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefillSeconds));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Вы исчерпали квоту запросов к API");
            log.warn("Превышен лимит запросов для эндпоинта {}", endpoint);
            return false;
        }
    }
}
