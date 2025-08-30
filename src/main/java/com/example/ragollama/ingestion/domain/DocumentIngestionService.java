package com.example.ragollama.ingestion.domain;

import com.example.ragollama.ingestion.api.dto.DocumentIngestionRequest;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.mappers.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Сервис для управления жизненным циклом документов.
 * <p>
 * В этой версии исправлена проблема гонки состояний (race condition)
 * путем отложенного запуска асинхронной обработки. Воркер теперь
 * вызывается только после успешного коммита основной транзакции.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionService {

    private final DocumentJobRepository jobRepository;
    private final DocumentMapper documentMapper;
    private final DocumentProcessingWorker documentProcessingWorker;

    /**
     * Сохраняет метаданные о документе и регистрирует задачу на асинхронную обработку
     * и индексацию, которая будет запущена после успешного завершения текущей транзакции.
     *
     * @param request DTO с данными документа.
     * @return Уникальный идентификатор (UUID) созданной задачи на обработку (Job ID).
     */
    @Transactional
    public UUID scheduleDocumentIngestion(DocumentIngestionRequest request) {
        log.info("Получен запрос на индексацию документа: '{}'", request.sourceName());
        DocumentJob newJob = documentMapper.toNewDocumentJob(request);
        DocumentJob savedJob = jobRepository.save(newJob);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.debug("Транзакция для Job ID {} успешно закоммичена. Запуск асинхронного воркера.", savedJob.getId());
                documentProcessingWorker.processDocument(savedJob.getId());
            }
        });
        log.info("Документ '{}' успешно сохранен. Асинхронная обработка будет запущена после коммита. Job ID: {}",
                savedJob.getSourceName(), savedJob.getId());
        return savedJob.getId();
    }
}
