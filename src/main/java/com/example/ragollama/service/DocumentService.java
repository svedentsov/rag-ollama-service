package com.example.ragollama.service;

import com.example.ragollama.dto.DocumentRequest;
import com.example.ragollama.entity.DocumentJob;
import com.example.ragollama.mapper.DocumentMapper;
import com.example.ragollama.repository.DocumentJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Сервис для управления жизненным циклом документов.
 * Отвечает за прием запросов на индексацию и постановку их в очередь для фоновой обработки.
 * Этот сервис является чистым оркестратором, делегируя преобразование данных мапперу, а сохранение - репозиторию.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentJobRepository jobRepository;
    private final DocumentMapper documentMapper;

    /**
     * Ставит документ в очередь на асинхронную индексацию.
     *
     * @param request DTO с данными документа.
     * @return Уникальный ID созданной задачи (Job ID).
     */
    @Transactional
    public UUID scheduleDocumentIngestion(DocumentRequest request) {
        log.info("Получен запрос на постановку в очередь документа: '{}'", request.sourceName());
        DocumentJob newJob = documentMapper.toNewDocumentJob(request);
        DocumentJob savedJob = jobRepository.save(newJob);
        log.info("Документ '{}' успешно поставлен в очередь. Job ID: {}", savedJob.getSourceName(), savedJob.getId());
        return savedJob.getId();
    }
}
