package com.example.ragollama.ingestion.domain;

import com.example.ragollama.agent.config.RabbitMqConfig;
import com.example.ragollama.indexing.IndexingPipelineService;
import com.example.ragollama.indexing.IndexingRequest;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.domain.model.JobStatus;
import com.example.ragollama.shared.exception.ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Компонент-слушатель RabbitMQ, отвечающий за асинхронную обработку задач индексации.
 * <p>
 * Этот класс заменяет собой связку `DocumentProcessingWorker` и `DocumentJobScheduler`.
 * Он работает в событийно-ориентированной модели, реагируя на сообщения в очереди,
 * что является более эффективным и масштабируемым подходом, чем опрос БД.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionMessageListener {

    private final DocumentJobRepository jobRepository;
    private final IndexingPipelineService indexingPipelineService;

    /**
     * Слушает очередь {@code document.ingestion.queue} и обрабатывает входящие задачи.
     * <p>
     * В случае неисправимой ошибки выбрасывает {@link AmqpRejectAndDontRequeueException},
     * что (при правильной настройке) заставит RabbitMQ переместить сообщение в DLQ.
     *
     * @param jobId Идентификатор задачи, полученный из сообщения.
     */
    @RabbitListener(queues = RabbitMqConfig.DOCUMENT_INGESTION_QUEUE)
    @Transactional
    public void onDocumentIngestionRequested(UUID jobId) {
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-" + jobId.toString().substring(0, 8))) {
            log.info("Получена задача на индексацию из RabbitMQ. Job ID: {}", jobId);

            DocumentJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ProcessingException("Задача с ID " + jobId + " не найдена в БД."));

            // Проверка для идемпотентности: не обрабатываем уже завершенные задачи
            if (job.getStatus() != JobStatus.PENDING) {
                log.warn("Задача {} уже находится в статусе {}. Пропуск обработки.", jobId, job.getStatus());
                return;
            }

            job.setStatus(JobStatus.PROCESSING);
            jobRepository.save(job);

            try {
                IndexingRequest indexingRequest = new IndexingRequest(
                        job.getId().toString(),
                        job.getSourceName(),
                        job.getTextContent(),
                        job.getMetadata()
                );
                indexingPipelineService.process(indexingRequest);

                job.markAsCompleted();
                jobRepository.save(job);
            } catch (Exception e) {
                log.error("Критическая ошибка при обработке документа. Job ID: {}", jobId, e);
                job.markAsFailed(e.getMessage());
                jobRepository.save(job);
                // Сигнализируем RabbitMQ, что сообщение не нужно возвращать в очередь
                throw new AmqpRejectAndDontRequeueException("Ошибка обработки документа " + jobId, e);
            }
        }
    }
}
