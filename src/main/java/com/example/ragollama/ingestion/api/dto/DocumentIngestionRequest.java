package com.example.ragollama.ingestion.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO для запроса на загрузку нового документа в систему.
 * <p>
 * В этой версии добавлены поля `isPublic` и `allowedRoles` для
 * управления доступом к документу на уровне RBAC.
 *
 * @param sourceName   Уникальное имя или идентификатор источника документа.
 * @param text         Полный текст документа для индексации.
 * @param metadata     Опциональная карта с дополнительными бизнес-метаданными.
 * @param isPublic     Флаг, указывающий, является ли документ общедоступным.
 * @param allowedRoles Список ролей, которым разрешен доступ к этому документу.
 */
@Schema(description = "DTO для загрузки нового документа для RAG с контролем доступа")
public record DocumentIngestionRequest(
        @Schema(description = "Имя источника документа", requiredMode = Schema.RequiredMode.REQUIRED, example = "JIRA-123.txt")
        @NotBlank(message = "Имя источника не может быть пустым")
        @Size(max = 255, message = "Имя источника не должно превышать 255 символов")
        String sourceName,

        @Schema(description = "Полный текст документа", requiredMode = Schema.RequiredMode.REQUIRED, example = "При нажатии на кнопку 'Сохранить' приложение падает...")
        @NotBlank(message = "Текст документа не может быть пустым")
        String text,

        @Schema(description = "Опциональная карта с метаданными для фильтрации", example = "{\"doc_type\": \"test_case\", \"priority\": \"High\"}")
        Map<String, Object> metadata,

        @Schema(description = "Является ли документ общедоступным", defaultValue = "false")
        boolean isPublic,

        @Schema(description = "Список ролей, имеющих доступ к документу", example = "[\"ROLE_ADMIN\", \"ROLE_DEV\"]")
        java.util.List<String> allowedRoles
) {
}
