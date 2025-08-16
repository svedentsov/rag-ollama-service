package com.example.ragollama.service;

import com.example.ragollama.entity.DocumentJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Планировщик, отвечающий за запуск процесса индексации документов.
 * Этот класс является "чистым" оркестратором. Его единственная задача -
 * периодически запрашивать новую задачу у {@link DocumentJobService},
 * выполнять ресурсоемкую обработку, а затем делегировать обновление статуса
 * обратно в {@link DocumentJobService}. Он не управляет транзакциями напрямую.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionScheduler {

    private final DocumentJobService documentJobService;
    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;

    /**
     * Периодически запускает процесс обработки ожидающих документов.
     * Метод сначала атомарно "захватывает" одну задачу через {@link DocumentJobService},
     * затем выполняет индексацию, и в конце обновляет статус задачи.
     * Аннотация {@code @CacheEvict} обеспечивает сброс кэша результатов поиска
     * после успешной индексации нового документа.
     *
     * @return Возвращает {@code true}, если документ был успешно обработан,
     * чтобы активировать {@code @CacheEvict}. В противном случае {@code false}.
     */
    @Scheduled(fixedDelayString = "${app.ingestion.scheduler.delay-ms:10000}")
    @CacheEvict(value = "vector_search_results", allEntries = true, condition = "#result == true")
    public boolean processPendingDocument() {
        // Шаг 1: Атомарно "захватываем" задачу. Это короткая, изолированная транзакция.
        return documentJobService.claimNextPendingJob().map(job -> {
            // Устанавливаем MDC для сквозной трассировки в логах этой фоновой задачи
            try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-" + job.getId().toString().substring(0, 8))) {
                processJob(job);
                return true; // Возвращаем true для срабатывания CacheEvict
            } catch (Exception e) {
                // Глобальный перехватчик на случай непредвиденных ошибок в processJob
                log.error("Критическая ошибка при обработке задачи {}. Отмечаем как FAILED.", job.getId(), e);
                documentJobService.markAsFailed(job.getId(), e.getMessage());
                return false;
            }
        }).orElse(false); // Возвращаем false, если не было задач для обработки
    }

    /**
     * Выполняет основную логику обработки одной задачи.
     * Этот метод выполняется вне транзакции к реляционной БД.
     * Он отвечает за ресурсоемкие операции.
     *
     * @param job Задача для обработки.
     */
    private void processJob(DocumentJob job) {
        UUID jobId = job.getId();
        log.info("Начинается обработка документа. Job ID: {}, Source: {}", jobId, job.getSourceName());
        try {
            // Шаг 2: Выполняем ресурсоемкие операции (не в транзакции).
            Document document = new Document(
                    job.getTextContent(),
                    Map.of("source", job.getSourceName(), "documentId", jobId.toString()));
            List<Document> chunks = tokenTextSplitter.apply(List.of(document));
            log.info("Документ '{}' (Job ID: {}) разделен на {} чанков.", job.getSourceName(), jobId, chunks.size());
            vectorStore.add(chunks);
            log.info("Успешно добавлено {} чанков в VectorStore для Job ID: {}", chunks.size(), jobId);
            // Шаг 3: Обновляем статус в короткой, изолированной транзакции.
            documentJobService.markAsCompleted(jobId);
        } catch (Exception e) {
            log.error("Ошибка при обработке документа. Job ID: {}", jobId, e);
            // Шаг 3 (альтернативный): Обновляем статус в короткой, изолированной транзакции.
            documentJobService.markAsFailed(jobId, e.getMessage());
            // Перевыбрасываем исключение, чтобы оно было поймано в вызывающем методе processPendingDocument
            throw new RuntimeException("Ошибка при обработке задачи " + jobId, e);
        }
    }
}
