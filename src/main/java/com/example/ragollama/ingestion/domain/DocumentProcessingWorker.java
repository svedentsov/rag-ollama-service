package com.example.ragollama.ingestion.domain;

import com.example.ragollama.ingestion.TextSplitterService;
import com.example.ragollama.ingestion.cleaning.DataCleaningService;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.domain.model.JobStatus;
import com.example.ragollama.shared.caching.VectorCacheService;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.security.PiiRedactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Асинхронный воркер для обработки и индексации документов.
 * <p>
 * Эта версия интегрирована с {@link PiiRedactionService} для обеспечения
 * безопасности данных. Маскирование PII и секретов теперь является
 * первым шагом в конвейере обработки.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingWorker {

    private final DocumentJobRepository jobRepository;
    private final VectorStore vectorStore;
    private final TextSplitterService textSplitterService;
    private final VectorCacheService vectorCacheService;
    private final DataCleaningService dataCleaningService;
    private final PiiRedactionService piiRedactionService;

    /**
     * Асинхронно выполняет полный цикл обработки одного документа.
     *
     * @param jobId Идентификатор задачи для обработки.
     */
    @Async("applicationTaskExecutor")
    public void processDocument(UUID jobId) {
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-" + jobId.toString().substring(0, 8))) {
            log.info("Начата асинхронная обработка документа. Job ID: {}", jobId);
            updateJobStatus(jobId, JobStatus.PROCESSING, null);
            DocumentJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ProcessingException("Задача с ID " + jobId + " не найдена после захвата."));

            // ШАГ 1: Маскирование чувствительных данных.
            String redactedText = piiRedactionService.redact(job.getTextContent());

            // ШАГ 2: Очистка текста от HTML и прочего "шума".
            String cleanedText = dataCleaningService.cleanDocumentText(redactedText);

            // ШАГ 3: Подготовка метаданных и разбиение на чанки.
            Map<String, Object> metadata = new HashMap<>();
            if (job.getMetadata() != null) {
                metadata.putAll(job.getMetadata());
            }
            metadata.put("source", job.getSourceName());
            metadata.put("documentId", job.getId().toString());
            List<Document> chunks = textSplitterService.split(new Document(cleanedText, metadata));
            log.info("Создано {} чанков для документа '{}' с помощью кастомного сплиттера.", chunks.size(), job.getSourceName());

            // ШАГ 4: Индексация и очистка кэша.
            if (!chunks.isEmpty()) {
                vectorStore.add(chunks);
                vectorCacheService.evictAll();
                log.info("Документ '{}' успешно проиндексирован, добавлено {} чанков.", job.getSourceName(), chunks.size());
            } else {
                log.warn("Для документа '{}' не было создано ни одного чанка.", job.getSourceName());
            }
            updateJobStatus(jobId, JobStatus.COMPLETED, null);
        } catch (Exception e) {
            log.error("Критическая ошибка при обработке документа. Job ID: {}", jobId, e);
            updateJobStatus(jobId, JobStatus.FAILED, e.getMessage());
        }
    }

    /**
     * Обновляет статус задачи в отдельной транзакции.
     *
     * @param jobId        ID задачи.
     * @param newStatus    Новый статус.
     * @param errorMessage Сообщение об ошибке (если есть).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobStatus(UUID jobId, JobStatus newStatus, String errorMessage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(newStatus);
            if (newStatus == JobStatus.FAILED) {
                job.setErrorMessage(errorMessage);
            }
            jobRepository.save(job);
        });
    }
}
