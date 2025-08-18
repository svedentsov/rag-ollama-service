package com.example.ragollama.shared.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления ограничением частоты запросов (rate limiting).
 * Использует библиотеку Bucket4j. Конфигурация лимитов загружается
 * из {@code application.yml} с префиксом {@code app.rate-limiting}.
 * Реализация хранит "ведра" (buckets) в памяти ({@link ConcurrentHashMap}),
 * что подходит для одного инстанса приложения.
 */
@Getter
@Setter
@Service
@ConfigurationProperties(prefix = "app.rate-limiting")
public class RateLimitingService {

    /**
     * Флаг, включающий или отключающий rate limiting.
     */
    private boolean enabled;

    /**
     * Список конфигураций лимитов для различных эндпоинтов.
     */
    private List<Limit> limits;

    /**
     * Кэш для хранения созданных "ведер" (buckets). Ключ - URI эндпоинта.
     */
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Находит или создает "ведро" для заданного эндпоинта.
     *
     * @param endpoint URI эндпоинта (например, "/api/v1/rag/query").
     * @return {@link Bucket} для данного эндпоинта или {@code null}, если лимит не настроен.
     */
    public Bucket resolveBucket(String endpoint) {
        if (!enabled) {
            return null;
        }
        // Ищем конфигурацию лимита для данного эндпоинта
        return limits.stream()
                .filter(limit -> endpoint.equalsIgnoreCase(limit.getEndpoint()))
                .findFirst()
                .map(limit -> cache.computeIfAbsent(endpoint, e -> createBucket(limit)))
                .orElse(null);
    }

    /**
     * Создает новое "ведро" (bucket) на основе конфигурации лимита.
     *
     * @param limit Конфигурация лимита.
     * @return Новый экземпляр {@link Bucket}.
     */
    private Bucket createBucket(Limit limit) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit.getCapacity())
                .refillGreedy(limit.getCapacity(), Duration.ofMinutes(limit.getRefillPeriodMinutes()))
                .build();

        return Bucket.builder().addLimit(bandwidth).build();
    }

    /**
     * Внутренний статический класс для хранения конфигурации одного лимита.
     */
    @Getter
    @Setter
    public static class Limit {
        private String endpoint;
        private long capacity; // Количество запросов
        private long refillPeriodMinutes; // Период пополнения в минутах
    }
}
