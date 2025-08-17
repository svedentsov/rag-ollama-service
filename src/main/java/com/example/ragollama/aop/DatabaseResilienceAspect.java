package com.example.ragollama.aop;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Аспект для применения политик отказоустойчивости к операциям с базой данных.
 * <p>
 * Этот аспект перехватывает все вызовы методов, аннотированных
 * {@link ResilientDatabaseOperation}, и оборачивает их в Circuit Breaker и Retry.
 * Это позволяет централизованно управлять обработкой транзиентных сбоев
 * при работе с БД, не загрязняя бизнес-логику репозиториев.
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
     * Загружает именованные конфигурации из реестров для дальнейшего использования.
     */
    @PostConstruct
    public void init() {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(DATABASE_CONFIG_NAME);
        this.retry = retryRegistry.retry(DATABASE_CONFIG_NAME);
    }

    /**
     * Совет (Advice), который оборачивает выполнение целевого метода.
     * <p>
     * Он создает декорированную цепочку вызовов, применяя сначала
     * политику Retry, а затем Circuit Breaker к исходной операции.
     *
     * @param joinPoint Точка соединения, представляющая вызов метода.
     * @return Результат выполнения исходного метода.
     * @throws Throwable если выполнение метода завершилось исключением после
     *                   всех попыток повторения, или если Circuit Breaker разомкнут.
     */
    @Around("@annotation(com.example.ragollama.aop.ResilientDatabaseOperation)")
    public Object applyResilience(ProceedingJoinPoint joinPoint) throws Throwable {
        Supplier<Object> supplier = () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                // Оборачиваем проверяемое исключение в непроверяемое,
                // так как декораторы Resilience4j работают с Supplier<T>,
                // который не может выбрасывать проверяемые исключения.
                throw new RuntimeException(throwable);
            }
        };

        // Создаем цепочку декораторов: сначала Retry, затем CircuitBreaker
        Supplier<Object> decoratedSupplier = Retry.decorateSupplier(retry, supplier);
        decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, decoratedSupplier);

        try {
            return decoratedSupplier.get();
        } catch (RuntimeException e) {
            // Разворачиваем RuntimeException, чтобы выбросить исходное исключение
            // наверх. Это важно для сохранения оригинального типа ошибки.
            if (e.getCause() != null) {
                throw e.getCause();
            }
            throw e;
        }
    }
}
