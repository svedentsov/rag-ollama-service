package com.example.ragollama.ingestion.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO для запроса на загрузку нового документа в систему.
 * <p>
 * В этой версии добавлено опциональное поле `metadata` для возможности
 * обогащения документов кастомными тегами на этапе загрузки.
 *
 * @param sourceName Уникальное имя или идентификатор источника документа.
 * @param text       Полный текст документа для индексации.
 * @param metadata   Опциональная карта с дополнительными метаданными (например, `{"doc_type": "test_case"}`).
 */
@Schema(description = "DTO для загрузки нового документа для RAG")
public record DocumentIngestionRequest(
        @Schema(description = "Имя источника документа", requiredMode = Schema.RequiredMode.REQUIRED, example = "JIRA-123.txt")
        @NotBlank(message = "Имя источника не может быть пустым")
        @Size(max = 255, message = "Имя источника не должно превышать 255 символов")
        String sourceName,

        @Schema(description = "Полный текст документа", requiredMode = Schema.RequiredMode.REQUIRED, example = "При нажатии на кнопку 'Сохранить' приложение падает...")
        @NotBlank(message = "Текст документа не может быть пустым")
        String text,

        @Schema(description = "Опциональная карта с метаданными для фильтрации", example = "{\"doc_type\": \"test_case\", \"priority\": \"High\"}")
        Map<String, Object> metadata
) {
}
