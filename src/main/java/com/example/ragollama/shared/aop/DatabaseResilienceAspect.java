package com.example.ragollama.shared.aop;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Аспект для применения политик отказоустойчивости, адаптированный для реактивного стека.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseResilienceAspect {

    private static final String DATABASE_CONFIG_NAME = "database";

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    private CircuitBreaker circuitBreaker;
    private Retry retry;

    /**
     * Инициализирует компоненты Resilience4j после создания бина.
     */
    @PostConstruct
    public void init() {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(DATABASE_CONFIG_NAME);
        this.retry = retryRegistry.retry(DATABASE_CONFIG_NAME);
    }

    /**
     * Совет, который оборачивает выполнение целевого метода.
     *
     * @param joinPoint Точка соединения, представляющая вызов метода.
     * @return Результат выполнения метода.
     * @throws Throwable если выполнение завершилось исключением.
     */
    @Around("@annotation(com.example.ragollama.shared.aop.ResilientDatabaseOperation)")
    public Object applyResilience(ProceedingJoinPoint joinPoint) throws Throwable {
        Object returnValue = joinPoint.proceed();

        if (returnValue instanceof Mono<?> mono) {
            log.trace("Применение реактивных операторов Resilience4j к Mono");
            return mono.transformDeferred(RetryOperator.of(retry))
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
        }

        if (returnValue instanceof Flux<?> flux) {
            log.trace("Применение реактивных операторов Resilience4j к Flux");
            return flux.transformDeferred(RetryOperator.of(retry))
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
        }

        // Fallback для не-реактивных методов (если такие остались)
        log.trace("Применение блокирующих декораторов Resilience4j для синхронного метода");
        Supplier<Object> supplier = () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };

        Supplier<Object> decoratedSupplier = Retry.decorateSupplier(retry, supplier);
        decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, decoratedSupplier);

        try {
            return decoratedSupplier.get();
        } catch (RuntimeException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            throw e;
        }
    }
}
