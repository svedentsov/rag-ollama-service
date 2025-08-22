package com.example.ragollama.ingestion.domain.scheduler;

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
 * В этой версии планировщик больше не запускает обработку напрямую. Его
 * единственная задача — атомарно захватить пакет задач и опубликовать
 * событие в RabbitMQ. Это полностью отвязывает процесс поиска задач от
 * их выполнения, создавая надежную, асинхронную архитектуру.
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
            log.info("Захвачен пакет из {} задач. Публикация события 'JobBatchClaimedEvent'. IDs: {}", jobs.size(), jobIds);
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EVENTS_EXCHANGE,
                    RabbitMqConfig.JOB_BATCH_CLAIMED_ROUTING_KEY,
                    new JobBatchClaimedEvent(jobIds)
            );
        }
    }

    /**
     * DTO для события, информирующего о захвате нового пакета задач.
     *
     * @param jobIds Список ID задач в пакете.
     */
    public record JobBatchClaimedEvent(List<UUID> jobIds) {
    }
}
