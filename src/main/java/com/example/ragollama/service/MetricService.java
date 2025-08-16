package com.example.ragollama.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Сервис для централизованного управления метриками Micrometer.
 * Предоставляет удобные методы для инкремента счетчиков и измерения времени выполнения
 * операций, что упрощает сбор данных для мониторинга и observability.
 */
@Service
public class MetricService {

    private final MeterRegistry meterRegistry;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    /**
     * Конструктор, который инициализирует реестр метрик и создает основные счетчики.
     * <p>
     * Счетчики создаются один раз при старте приложения, что является
     * более эффективным подходом, чем их создание "на лету".
     *
     * @param meterRegistry Реестр метрик, предоставляемый Spring Boot Actuator.
     */
    public MetricService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.cacheHitCounter = Counter.builder("cache.requests")
                .tag("result", "hit")
                .description("Количество попаданий в кэш")
                .register(meterRegistry);
        this.cacheMissCounter = Counter.builder("cache.requests")
                .tag("result", "miss")
                .description("Количество промахов кэша")
                .register(meterRegistry);
    }

    /**
     * Измеряет и записывает время выполнения для заданной операции.
     *
     * @param metricName Имя таймера (метрики).
     * @param supplier   Операция (в виде {@link Supplier}), время выполнения которой нужно измерить.
     * @param <T>        Тип результата операции.
     * @return Результат выполнения операции.
     */
    public <T> T recordTimer(String metricName, Supplier<T> supplier) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return supplier.get();
        } finally {
            sample.stop(meterRegistry.timer(metricName));
        }
    }

    /**
     * Увеличивает счетчик попаданий в кэш.
     */
    public void incrementCacheHit() {
        cacheHitCounter.increment();
    }

    /**
     * Увеличивает счетчик промахов кэша.
     */
    public void incrementCacheMiss() {
        cacheMissCounter.increment();
    }

    /**
     * Увеличивает счетчик ошибок API для заданного HTTP-статуса.
     *
     * @param status HTTP-статус код ошибки.
     */
    public void incrementApiError(int status) {
        Counter.builder("api.errors.total")
                .tag("status", String.valueOf(status))
                .description("Общее количество ошибок API по коду статуса")
                .register(meterRegistry)
                .increment();
    }
}
