package com.example.ragollama.ingestion.domain.consumer;

import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.domain.scheduler.DocumentIngestionScheduler;
import com.example.ragollama.qaagent.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Слушатель RabbitMQ, который реагирует на событие захвата пакета задач.
 * <p>
 * Его единственная задача — разбить пакет на отдельные задачи и опубликовать
 * для каждой из них индивидуальное событие на обработку. Этот "разветвитель"
 * (fan-out) является ключевым элементом для обеспечения изоляции сбоев:
 * ошибка при обработке одного документа не повлияет на остальные в пакете.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobBatchConsumer {

    /**
     * Внедряем JpaRepository напрямую для простой операции поиска.
     * Это избавляет от необходимости в дополнительном слое сервиса
     * для такой тривиальной задачи.
     */
    private final JpaRepository<DocumentJob, UUID> jobRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Обрабатывает событие {@link DocumentIngestionScheduler.JobBatchClaimedEvent}.
     *
     * @param event Событие, содержащее список ID задач для обработки.
     */
    @RabbitListener(queues = RabbitMqConfig.JOB_BATCH_CLAIMED_QUEUE)
    public void handleJobBatchClaimed(DocumentIngestionScheduler.JobBatchClaimedEvent event) {
        log.info("Получено событие о захвате пакета из {} задач. Начинается публикация индивидуальных событий.", event.jobIds().size());
        List<DocumentJob> jobs = jobRepository.findAllById(event.jobIds());

        jobs.forEach(job -> {
            log.debug("Публикация события 'ProcessDocumentJobEvent' для Job ID: {}", job.getId());
            var processEvent = new ProcessDocumentJobEvent(job.getId(), job.getSourceName(), job.getTextContent());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EVENTS_EXCHANGE,
                    RabbitMqConfig.DOCUMENT_PROCESSING_ROUTING_KEY,
                    processEvent
            );
        });
    }

    /**
     * DTO для события, инициирующего обработку одного документа.
     * Содержит все необходимые данные, чтобы обработчик был stateless.
     *
     * @param jobId       Уникальный ID задачи.
     * @param sourceName  Имя источника документа.
     * @param textContent Полный текст документа.
     */
    public record ProcessDocumentJobEvent(UUID jobId, String sourceName, String textContent) {
    }
}
