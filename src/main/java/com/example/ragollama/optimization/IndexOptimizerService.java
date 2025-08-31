package com.example.ragollama.optimization;

import com.example.ragollama.ingestion.domain.DocumentJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Сервис-агент, выполняющий фоновые задачи по оптимизации и очистке
 * векторного индекса. Этот "Memory Manager" обеспечивает долгосрочное
 * здоровье и актуальность базы знаний.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexOptimizerService {

    private final DocumentJobRepository documentJobRepository;
    private final VectorStoreRepository vectorStoreRepository;
    private final JdbcTemplate jdbcTemplate;
    private final IndexOptimizerProperties properties;
    private final AtomicBoolean isOptimizationRunning = new AtomicBoolean(false);

    /**
     * Асинхронно запускает полный цикл оптимизации индекса.
     * <p>
     * Включает в себя удаление "осиротевших" чанков и выполнение `VACUUM` на таблице.
     * Использует атомарную блокировку для предотвращения одновременного запуска
     * нескольких экземпляров задачи.
     */
    @Async("applicationTaskExecutor")
    public void runOptimizationAsync() {
        if (!isOptimizationRunning.compareAndSet(false, true)) {
            log.warn("Задача оптимизации индекса уже запущена. Пропуск текущего вызова.");
            return;
        }

        log.info("Начало задачи оптимизации индекса.");
        try {
            if (properties.getStaleDocumentDetection().isEnabled()) {
                cleanupStaleDocuments();
            }
            vacuumVectorStore();
            log.info("Задача оптимизации индекса успешно завершена.");
        } catch (Exception e) {
            log.error("Произошла ошибка во время оптимизации индекса.", e);
        } finally {
            isOptimizationRunning.set(false);
        }
    }

    /**
     * Обнаруживает и удаляет документы, которые были удалены из источника правды
     * (в данном примере - из таблицы `document_jobs`).
     * <p>
     * В реальной системе здесь могла бы быть логика сверки с внешним API (Confluence, Jira).
     * Для демонстрации, мы имитируем удаление, находя "старые" завершенные задачи.
     */
    @Transactional
    public void cleanupStaleDocuments() {
        log.info("Запуск этапа очистки устаревших документов...");
        // Например, удалим все, что было успешно обработано более 7 дней назад.
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(7);
        List<UUID> staleJobIds = documentJobRepository.findCompletedJobsBefore(threshold);

        if (staleJobIds.isEmpty()) {
            log.info("Устаревшие документы для удаления не найдены.");
            return;
        }

        log.warn("Обнаружено {} устаревших документов для удаления. IDs: {}",
                staleJobIds.size(), staleJobIds.stream().map(UUID::toString).collect(Collectors.joining(", ")));

        for (UUID jobId : staleJobIds) {
            // Удаляем связанные чанки из vector_store
            int deletedChunks = vectorStoreRepository.deleteByDocumentId(jobId);
            // Удаляем саму запись о задаче
            documentJobRepository.deleteById(jobId);
            log.info("Удалено {} чанков для документа с Job ID: {}", deletedChunks, jobId);
        }
        log.info("Очистка устаревших документов завершена.");
    }

    /**
     * Выполняет команду `VACUUM` на таблице `vector_store` для оптимизации хранения
     * и обновления статистики в PostgreSQL.
     */
    public void vacuumVectorStore() {
        log.info("Выполнение команды VACUUM для таблицы 'vector_store'...");
        try {
            jdbcTemplate.execute("VACUUM (VERBOSE, ANALYZE) vector_store;");
            log.info("Команда VACUUM для 'vector_store' успешно выполнена.");
        } catch (Exception e) {
            log.error("Ошибка при выполнении VACUUM для 'vector_store'.", e);
        }
    }
}
