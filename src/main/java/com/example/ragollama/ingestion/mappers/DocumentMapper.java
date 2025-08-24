package com.example.ragollama.ingestion.mappers;

import com.example.ragollama.ingestion.api.dto.DocumentIngestionRequest;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.domain.model.JobStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Компонент-маппер, отвечающий за преобразование DTO в доменные сущности.
 * <p>
 * Эта версия корректно переносит метаданные контроля доступа (RBAC)
 * из запроса в соответствующее поле сущности {@link DocumentJob}.
 */
@Component
public class DocumentMapper {

    /**
     * Преобразует DTO {@link DocumentIngestionRequest} в новую сущность {@link DocumentJob}.
     *
     * @param request DTO с данными для создания задачи.
     * @return Новая, не сохраненная в БД, сущность {@link DocumentJob} со статусом PENDING.
     */
    public DocumentJob toNewDocumentJob(DocumentIngestionRequest request) {
        Map<String, Object> finalMetadata = new HashMap<>();
        if (request.metadata() != null) {
            finalMetadata.putAll(request.metadata());
        }

        // Добавляем метаданные контроля доступа
        finalMetadata.put("isPublic", request.isPublic());
        if (request.allowedRoles() != null && !request.allowedRoles().isEmpty()) {
            finalMetadata.put("allowedRoles", request.allowedRoles());
        }

        return DocumentJob.builder()
                .sourceName(request.sourceName())
                .textContent(request.text())
                .status(JobStatus.PENDING)
                .metadata(finalMetadata)
                .build();
    }
}
