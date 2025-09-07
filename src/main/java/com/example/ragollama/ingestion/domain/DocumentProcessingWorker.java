package com.example.ragollama.ingestion.domain;

import com.example.ragollama.indexing.IndexingPipelineService;
import com.example.ragollama.indexing.IndexingRequest;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.shared.exception.ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Асинхронный воркер для обработки и индексации документов.
 * <p>
 * Этот компонент инкапсулирует всю бизнес-логику жизненного цикла одной задачи.
 * Метод {@code processDocument} выполняется в отдельном потоке из пула
 * `applicationTaskExecutor` и в собственной транзакции.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingWorker {

    private final DocumentJobRepository jobRepository;
    private final IndexingPipelineService indexingPipelineService;

    /**
     * Асинхронно и транзакционно выполняет полный цикл обработки одного документа.
     *
     * @param jobId Идентификатор задачи для обработки.
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public void processDocument(UUID jobId) {
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-" + jobId.toString().substring(0, 8))) {
            log.info("Начата асинхронная обработка документа. Job ID: {}", jobId);
            DocumentJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ProcessingException("Задача с ID " + jobId + " не найдена после захвата."));
            try {
                IndexingRequest indexingRequest = new IndexingRequest(
                        job.getId().toString(),
                        job.getSourceName(),
                        job.getTextContent(),
                        job.getMetadata()
                );
                indexingPipelineService.process(indexingRequest);
                job.markAsCompleted();
            } catch (Exception e) {
                log.error("Критическая ошибка при обработке документа. Job ID: {}", jobId, e);
                job.markAsFailed(e.getMessage());
            }
            jobRepository.save(job);

        } catch (Exception e) {
            log.error("Неустранимая ошибка на начальном этапе обработки Job ID: {}", jobId, e);
        }
    }
}
