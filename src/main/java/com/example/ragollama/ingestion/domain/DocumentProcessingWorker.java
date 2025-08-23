package com.example.ragollama.ingestion.domain;

import com.example.ragollama.ingestion.TextSplitterService;
import com.example.ragollama.ingestion.cleaning.DataCleaningService;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.domain.model.JobStatus;
import com.example.ragollama.shared.caching.VectorCacheService;
import com.example.ragollama.shared.exception.ProcessingException;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingWorker {

    private final DocumentJobRepository jobRepository;
    private final VectorStore vectorStore;
    private final TextSplitterService textSplitterService;
    private final VectorCacheService vectorCacheService;
    private final DataCleaningService dataCleaningService;

    @Async("applicationTaskExecutor")
    public void processDocument(UUID jobId) {
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-" + jobId.toString().substring(0, 8))) {
            log.info("Начата асинхронная обработка документа. Job ID: {}", jobId);
            updateJobStatus(jobId, JobStatus.PROCESSING, null);
            DocumentJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ProcessingException("Задача с ID " + jobId + " не найдена после захвата."));
            String cleanedText = dataCleaningService.cleanDocumentText(job.getTextContent());
            Map<String, Object> metadata = new HashMap<>();
            if (job.getMetadata() != null) {
                metadata.putAll(job.getMetadata());
            }
            metadata.put("source", job.getSourceName());
            metadata.put("documentId", job.getId().toString());
            List<Document> chunks = textSplitterService.split(new Document(cleanedText, metadata));
            log.info("Создано {} чанков для документа '{}' с помощью кастомного сплиттера.", chunks.size(), job.getSourceName());
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
