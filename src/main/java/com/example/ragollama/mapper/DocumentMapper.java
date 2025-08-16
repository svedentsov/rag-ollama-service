package com.example.ragollama.mapper;

import com.example.ragollama.dto.DocumentRequest;
import com.example.ragollama.entity.DocumentJob;
import com.example.ragollama.entity.JobStatus;
import org.springframework.stereotype.Component;

/**
 * Компонент-маппер, отвечающий за преобразование DTO в доменные сущности.
 * Этот класс инкапсулирует логику создания сущностей из запросов, следуя
 * Принципу единственной ответственности (SRP).
 * *
 * Для более сложных сценариев здесь можно использовать библиотеки, такие как MapStruct.
 */
@Component
public class DocumentMapper {

    /**
     * Преобразует DTO {@link DocumentRequest} в новую сущность {@link DocumentJob}.
     *
     * @param request DTO с данными для создания задачи.
     * @return Новая, не сохраненная в БД, сущность {@link DocumentJob} со статусом PENDING.
     */
    public DocumentJob toNewDocumentJob(DocumentRequest request) {
        return DocumentJob.builder()
                .sourceName(request.sourceName())
                .textContent(request.text())
                .status(JobStatus.PENDING)
                .build();
    }
}
