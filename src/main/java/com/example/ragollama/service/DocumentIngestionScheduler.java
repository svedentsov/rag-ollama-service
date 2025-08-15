package com.example.ragollama.service;

import com.example.ragollama.entity.DocumentJob;
import com.example.ragollama.entity.JobStatus;
import com.example.ragollama.repository.DocumentJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionScheduler {

    private final DocumentJobRepository jobRepository;
    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;

    @Scheduled(fixedDelayString = "${app.ingestion.scheduler.delay-ms:10000}")
    @Transactional
    @CacheEvict(value = "vector_search_results", allEntries = true)
    public void processPendingDocuments() {
        // === ИЗМЕНЕНИЕ ЗДЕСЬ ===
        // Теперь мы получаем Optional, а не List
        Optional<DocumentJob> pendingJobOptional = jobRepository.findFirstByStatusOrderByCreatedAt(JobStatus.PENDING);

        // Проверяем, есть ли задача в Optional. Если нет - выходим.
        if (pendingJobOptional.isEmpty()) {
            return; // Нет работы
        }

        // Извлекаем задачу из Optional
        DocumentJob job = pendingJobOptional.get();

        // Устанавливаем MDC для сквозной трассировки в логах этой фоновой задачи
        try {
            // Генерируем уникальный ID для этой сессии обработки на основе ID задачи
            MDC.put("requestId", "ingestion-" + job.getId().toString().substring(0, 8));

            log.info("Начинается обработка документа. Job ID: {}, Source: {}", job.getId(), job.getSourceName());

            job.setStatus(JobStatus.PROCESSING);
            jobRepository.save(job);

            try {
                // Создаем уникальный ID для всего документа
                String documentId = job.getId().toString();
                Document document = new Document(
                        job.getTextContent(),
                        Map.of("source", job.getSourceName(), "documentId", documentId)
                );

                List<Document> chunks = tokenTextSplitter.apply(List.of(document));
                log.info("Документ '{}' (Job ID: {}) разделен на {} чанков.", job.getSourceName(), job.getId(), chunks.size());

                vectorStore.add(chunks);
                log.info("Успешно добавлено {} чанков в VectorStore для Job ID: {}", chunks.size(), job.getId());

                job.setStatus(JobStatus.COMPLETED);
            } catch (Exception e) {
                log.error("Ошибка при обработке документа. Job ID: {}", job.getId(), e);
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage(e.getMessage());
            } finally {
                jobRepository.save(job);
            }
        } finally {
            // Обязательно очищаем MDC после завершения
            MDC.clear();
        }
    }
}
