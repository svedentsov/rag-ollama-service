package com.example.ragollama.ingestion.domain.scheduler;

import com.example.ragollama.ingestion.domain.DocumentProcessingService;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.domain.DocumentJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Планировщик, периодически запускающий пакетную индексацию документов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionScheduler {

    private final DocumentJobService documentJobService;
    private final DocumentProcessingService documentProcessingService;

    @Value("${app.ingestion.batch-size:10}")
    private int batchSize;

    /**
     * Периодически запускает процесс поиска и обработки пакета ожидающих документов.
     * Метод атомарно захватывает пакет задач и, если он не пуст, передает его на асинхронную обработку.
     */
    @Scheduled(fixedDelayString = "${app.ingestion.scheduler.delay-ms:10000}")
    public void schedulePendingDocumentProcessing() {
        log.trace("Планировщик проверяет наличие новых задач на индексацию...");
        List<DocumentJob> jobs = documentJobService.claimNextPendingJobBatch(batchSize);
        if (!jobs.isEmpty()) {
            log.info("Захвачен пакет из {} задач. Передаем в асинхронный исполнитель.", jobs.size());
            documentProcessingService.processBatch(jobs);
        }
    }
}
