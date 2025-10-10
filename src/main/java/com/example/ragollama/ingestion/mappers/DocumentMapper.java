package com.example.ragollama.ingestion.mappers;

import com.example.ragollama.ingestion.api.dto.DocumentIngestionRequest;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.domain.model.JobStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Компонент-маппер, отвечающий за преобразование DTO в доменные сущности.
 * <p>
 * Эта версия создает новую сущность {@link DocumentJob} с `id`, равным `null`.
 * Это является сигналом для Spring Data R2DBC о том, что необходимо выполнить
 * операцию `INSERT`, а генерацию UUID делегировать базе данных через
 * механизм `DEFAULT gen_random_uuid()`, определенный в SQL-миграции.
 */
@Component
public class DocumentMapper {

    /**
     * Преобразует DTO {@link DocumentIngestionRequest} в новую сущность {@link DocumentJob}.
     *
     * @param request DTO с данными для создания задачи.
     * @return Новая, не сохраненная в БД, сущность {@link DocumentJob} со статусом PENDING и `id = null`.
     */
    public DocumentJob toNewDocumentJob(DocumentIngestionRequest request) {
        Map<String, Object> finalMetadata = new HashMap<>();
        Optional.ofNullable(request.metadata()).ifPresent(finalMetadata::putAll);
        Optional.ofNullable(request.versionMetadata()).ifPresent(finalMetadata::putAll);

        finalMetadata.put("isPublic", request.isPublic());
        if (request.allowedRoles() != null && !request.allowedRoles().isEmpty()) {
            finalMetadata.put("allowedRoles", request.allowedRoles());
        }

        return DocumentJob.builder()
                // ID НЕ устанавливается здесь. Он будет сгенерирован базой данных.
                .sourceName(request.sourceName())
                .textContent(request.text())
                .status(JobStatus.PENDING)
                .metadata(finalMetadata)
                .build();
    }
}
