package com.example.ragollama.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для запроса на загрузку нового документа в систему.
 * Используется для передачи данных, необходимых для индексации документа
 * в векторном хранилище для последующего использования в RAG.
 *
 * @param sourceName Уникальное имя или идентификатор источника документа (например, имя файла).
 *                   Используется в метаданных для отслеживания происхождения информации.
 * @param text       Полный текст документа, который необходимо разбить на чанки и проиндексировать.
 */
@Schema(description = "DTO для загрузки нового документа для RAG")
public record DocumentRequest(
        @Schema(description = "Имя источника документа (например, 'wikipedia_spring_boot.txt')", requiredMode = Schema.RequiredMode.REQUIRED, example = "internal_doc_v1.txt")
        @NotBlank(message = "Имя источника не может быть пустым")
        @Size(max = 255, message = "Имя источника не должно превышать 255 символов")
        String sourceName,

        @Schema(description = "Полный текст документа", requiredMode = Schema.RequiredMode.REQUIRED, example = "Spring Boot - это фреймворк для...")
        @NotBlank(message = "Текст документа не может быть пустым")
        String text
) {
}
