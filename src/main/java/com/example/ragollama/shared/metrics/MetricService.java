package com.example.ragollama.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Сервис для централизованного управления метриками Micrometer.
 * <p>
 * Предоставляет удобные методы для инкремента счетчиков и измерения времени выполнения
 * операций. Включает метрики для RAG-конвейера, такие как количество найденных
 * документов и результаты проверки "обоснованности" (grounding) ответов.
 */
@Service
public class MetricService {

    private final MeterRegistry meterRegistry;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter emptyRetrievalCounter;
    private final Counter successfulRetrievalCounter;
    private final DistributionSummary retrievedDocumentsSummary;
    private final Counter groundedCounter;
    private final Counter ungroundedCounter;

    /**
     * Конструктор, который инициализирует реестр метрик и создает все необходимые счетчики.
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

        this.emptyRetrievalCounter = Counter.builder("rag.retrieval.results")
                .tag("status", "empty")
                .description("Количество RAG-запросов, для которых поиск не нашел ни одного документа.")
                .register(meterRegistry);

        this.successfulRetrievalCounter = Counter.builder("rag.retrieval.results")
                .tag("status", "found")
                .description("Количество RAG-запросов, для которых поиск нашел хотя бы один документ.")
                .register(meterRegistry);

        this.retrievedDocumentsSummary = DistributionSummary.builder("rag.retrieval.documents.count")
                .description("Распределение количества документов, найденных на этапе Retrieval.")
                .baseUnit("documents")
                .register(meterRegistry);

        this.groundedCounter = Counter.builder("rag.grounding.checks")
                .tag("result", "grounded")
                .description("Количество ответов, которые были успешно верифицированы как основанные на контексте.")
                .register(meterRegistry);

        this.ungroundedCounter = Counter.builder("rag.grounding.checks")
                .tag("result", "ungrounded")
                .description("Количество ответов, которые не прошли проверку на 'обоснованность' (потенциальные галлюцинации).")
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

    /**
     * Записывает метрики, связанные с результатом этапа извлечения (Retrieval).
     *
     * @param count количество найденных документов.
     */
    public void recordRetrievedDocumentsCount(int count) {
        if (count == 0) {
            emptyRetrievalCounter.increment();
        } else {
            successfulRetrievalCounter.increment();
        }
        retrievedDocumentsSummary.record(count);
    }

    /**
     * Записывает результат проверки "обоснованности" (grounding) ответа.
     *
     * @param isGrounded {@code true}, если ответ основан на контексте, иначе {@code false}.
     */
    public void recordGroundingResult(boolean isGrounded) {
        if (isGrounded) {
            groundedCounter.increment();
        } else {
            ungroundedCounter.increment();
        }
    }
}
