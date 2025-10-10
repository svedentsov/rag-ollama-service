package com.example.ragollama.ingestion.domain;

import com.example.ragollama.ingestion.api.dto.DocumentIngestionRequest;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.mappers.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Сервис для управления жизненным циклом документов, адаптированный для R2DBC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionService {

    private final DocumentJobRepository jobRepository;
    private final DocumentMapper documentMapper;
    private final DocumentProcessingWorker documentProcessingWorker;

    @Transactional
    public Mono<UUID> scheduleDocumentIngestion(DocumentIngestionRequest request) {
        log.info("Получен запрос на индексацию документа: '{}'", request.sourceName());
        DocumentJob newJob = documentMapper.toNewDocumentJob(request);
        return jobRepository.save(newJob)
                .doOnSuccess(savedJob -> {
                    log.debug("Транзакция для Job ID {} успешно закоммичена. Запуск асинхронного воркера.", savedJob.getId());
                    documentProcessingWorker.processDocument(savedJob.getId()).subscribe();
                    log.info("Документ '{}' успешно сохранен. Задача на индексацию запущена. Job ID: {}",
                            savedJob.getSourceName(), savedJob.getId());
                })
                .map(DocumentJob::getId);
    }
}
