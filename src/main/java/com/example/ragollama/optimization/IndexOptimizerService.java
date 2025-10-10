package com.example.ragollama.optimization;

import com.example.ragollama.ingestion.domain.DocumentJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис-агент для оптимизации индекса, адаптированный для R2DBC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexOptimizerService {

    private final DocumentJobRepository documentJobRepository;
    private final VectorStoreRepository vectorStoreRepository;
    private final DatabaseClient databaseClient;
    private final IndexOptimizerProperties properties;
    private final AtomicBoolean isOptimizationRunning = new AtomicBoolean(false);

    /**
     * Асинхронно запускает полный цикл оптимизации индекса.
     */
    @Async("applicationTaskExecutor")
    public void runOptimizationAsync() {
        if (!isOptimizationRunning.compareAndSet(false, true)) {
            log.warn("Задача оптимизации уже запущена. Пропуск.");
            return;
        }
        log.info("Начало задачи оптимизации индекса.");
        Mono.just(properties.getStaleDocumentDetection().isEnabled())
                .filter(Boolean::booleanValue)
                .flatMap(enabled -> cleanupStaleDocuments())
                .then(vacuumVectorStore())
                .doOnSuccess(v -> log.info("Задача оптимизации индекса успешно завершена."))
                .doOnError(e -> log.error("Ошибка во время оптимизации индекса.", e))
                .doFinally(signal -> isOptimizationRunning.set(false))
                .subscribe();
    }

    /**
     * Выполняет очистку устаревших документов и связанных с ними чанков.
     *
     * @return {@link Mono}, завершающийся после выполнения операции.
     */
    @Transactional
    public Mono<Void> cleanupStaleDocuments() {
        log.info("Запуск очистки устаревших документов...");
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(7);
        return documentJobRepository.findCompletedJobsBefore(threshold)
                .map(java.util.UUID::toString)
                .collectList()
                .flatMap(staleJobIds -> {
                    if (staleJobIds.isEmpty()) {
                        log.info("Устаревшие документы не найдены.");
                        return Mono.empty();
                    }
                    log.warn("Обнаружено {} устаревших документов для удаления.", staleJobIds.size());
                    return documentJobRepository.deleteAllById(staleJobIds.stream().map(UUID::fromString).toList())
                            .then(vectorStoreRepository.deleteByDocumentIds(staleJobIds))
                            .doOnSuccess(deletedCount -> log.info("Удалено {} чанков для {} документов.", deletedCount, staleJobIds.size()));
                })
                .doOnSuccess(v -> log.info("Очистка устаревших документов завершена."))
                .then();
    }

    /**
     * Выполняет команду VACUUM для таблицы vector_store.
     *
     * @return {@link Mono}, завершающийся после выполнения операции.
     */
    public Mono<Void> vacuumVectorStore() {
        log.info("Выполнение VACUUM для 'vector_store'...");
        return databaseClient.sql("VACUUM (VERBOSE, ANALYZE) vector_store;")
                .then()
                .doOnSuccess(v -> log.info("VACUUM для 'vector_store' успешно выполнен."))
                .doOnError(e -> log.error("Ошибка при выполнении VACUUM.", e));
    }
}
