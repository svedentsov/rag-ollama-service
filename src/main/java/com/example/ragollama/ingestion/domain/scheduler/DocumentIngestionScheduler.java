package com.example.ragollama.ingestion.domain.scheduler;

import com.example.ragollama.ingestion.consumer.DocumentProcessingConsumer;
import com.example.ragollama.ingestion.domain.DocumentJobService;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.qaagent.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Планировщик, периодически запускающий пакетную индексацию документов.
 * <p>
 * Его единственная задача — атомарно захватить пакет ожидающих задач
 * и опубликовать одно событие в RabbitMQ для их пакетной обработки.
 * Это полностью отвязывает процесс поиска задач от их выполнения,
 * создавая надежную, асинхронную и эффективную архитектуру.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionScheduler {

    private final DocumentJobService documentJobService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.ingestion.batch-size:10}")
    private int batchSize;

    /**
     * Периодически запускает процесс поиска и постановки в очередь пакета
     * ожидающих документов.
     */
    @Scheduled(fixedDelayString = "${app.ingestion.scheduler.delay-ms:10000}")
    public void schedulePendingDocumentProcessing() {
        log.trace("Планировщик проверяет наличие новых задач на индексацию...");
        List<DocumentJob> jobs = documentJobService.claimNextPendingJobBatch(batchSize);

        if (!jobs.isEmpty()) {
            List<UUID> jobIds = jobs.stream().map(DocumentJob::getId).toList();
            log.info("Захвачен пакет из {} задач. Публикация события 'BatchProcessingRequestedEvent'. IDs: {}", jobs.size(), jobIds);
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EVENTS_EXCHANGE,
                    RabbitMqConfig.DOCUMENT_PROCESSING_ROUTING_KEY,
                    new DocumentProcessingConsumer.BatchProcessingRequestedEvent(jobIds)
            );
        }
    }
}
