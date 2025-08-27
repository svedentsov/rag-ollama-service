package com.example.ragollama.ingestion.domain;

import com.example.ragollama.indexing.IndexingPipelineService;
import com.example.ragollama.indexing.IndexingRequest;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.domain.model.JobStatus;
import com.example.ragollama.shared.exception.ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Асинхронный воркер для обработки и индексации документов.
 * <p>
 * В этой версии воркер делегирует всю основную логику конвейера
 * переиспользуемому сервису {@link IndexingPipelineService},
 * отвечая только за управление жизненным циклом {@link DocumentJob}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingWorker {

    private final DocumentJobRepository jobRepository;
    private final IndexingPipelineService indexingPipelineService;

    /**
     * Асинхронно выполняет полный цикл обработки одного документа.
     * <p>
     * Метод извлекает задачу из базы данных, преобразует ее в универсальный
     * запрос на индексацию и передает его в {@link IndexingPipelineService}.
     * Управляет статусом задачи в БД.
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
            // Преобразуем Job в универсальный запрос и делегируем выполнение
            IndexingRequest indexingRequest = new IndexingRequest(
                    job.getId().toString(),
                    job.getSourceName(),
                    job.getTextContent(),
                    job.getMetadata()
            );
            indexingPipelineService.process(indexingRequest);
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
