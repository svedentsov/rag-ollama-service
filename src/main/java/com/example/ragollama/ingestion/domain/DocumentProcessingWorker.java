package com.example.ragollama.ingestion.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.indexing.IndexingPipelineService;
import com.example.ragollama.indexing.IndexingRequest;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.optimization.DocumentEnhancerAgent;
import com.example.ragollama.optimization.model.EnhancedMetadata;
import com.example.ragollama.shared.exception.ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Асинхронный воркер для обработки и индексации документов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingWorker {

    private final DocumentJobRepository jobRepository;
    private final IndexingPipelineService indexingPipelineService;
    private final DocumentEnhancerAgent enhancerAgent;

    /**
     * Асинхронно и транзакционно выполняет полный цикл обработки одного документа.
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public void processDocument(UUID jobId) {
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-" + jobId.toString().substring(0, 8))) {
            log.info("Начата асинхронная обработка документа. Job ID: {}", jobId);
            DocumentJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ProcessingException("Задача с ID " + jobId + " не найдена после захвата."));
            try {
                EnhancedMetadata enhancedMetadata = (EnhancedMetadata) enhancerAgent
                        .execute(new AgentContext(Map.of("document_text", job.getTextContent())))
                        .join()
                        .details()
                        .get("enhancedMetadata");

                Map<String, Object> finalMetadata = new HashMap<>(job.getMetadata());
                finalMetadata.put("summary", enhancedMetadata.summary());
                finalMetadata.put("keywords", enhancedMetadata.keywords());
                IndexingRequest indexingRequest = new IndexingRequest(
                        job.getId().toString(),
                        job.getSourceName(),
                        job.getTextContent(),
                        finalMetadata
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
