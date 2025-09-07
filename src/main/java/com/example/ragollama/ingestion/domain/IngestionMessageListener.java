package com.example.ragollama.ingestion.domain;

import com.example.ragollama.agent.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Компонент-слушатель RabbitMQ, отвечающий за асинхронную обработку задач индексации.
 * <p>
 * Этот класс реализует паттерн "Диспетчер". Его единственная ответственность —
 * принять сообщение из очереди и немедленно делегировать его обработку
 * асинхронному воркеру, освобождая поток слушателя для приема новых сообщений.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionMessageListener {

    private final DocumentProcessingWorker documentProcessingWorker;

    /**
     * Слушает очередь {@code document.ingestion.queue} и запускает асинхронную обработку.
     *
     * @param jobId Идентификатор задачи, полученный из сообщения.
     */
    @RabbitListener(queues = RabbitMqConfig.DOCUMENT_INGESTION_QUEUE)
    public void onDocumentIngestionRequested(UUID jobId) {
        log.info("Получена задача на индексацию из RabbitMQ. Job ID: {}. Делегирование в асинхронный воркер.", jobId);
        documentProcessingWorker.processDocument(jobId);
    }
}
