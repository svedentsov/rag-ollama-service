package com.example.ragollama.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO для RAG-запроса.
 *
 * @param query               Вопрос пользователя.
 * @param topK                Количество наиболее релевантных документов для извлечения.
 * @param similarityThreshold Минимальный порог схожести для документов (от 0.0 до 1.0).
 */
@Schema(description = "DTO для RAG-запроса")
public record RagQueryRequest(
        @Schema(description = "Вопрос пользователя", requiredMode = Schema.RequiredMode.REQUIRED, example = "Что такое Spring Boot?")
        @NotBlank(message = "Запрос не может быть пустым")
        @Size(max = 2048, message = "Запрос не должен превышать 2048 символов")
        String query,

        @Schema(description = "Количество извлекаемых чанков", defaultValue = "4", example = "5")
        @NotNull @Min(1) @Max(10)
        Integer topK,

        @Schema(description = "Порог схожести (0.0-1.0)", defaultValue = "0.7", example = "0.75")
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        Double similarityThreshold
) {
    // Конструктор по умолчанию для установки значений, если они не предоставлены
    public RagQueryRequest {
        if (topK == null) topK = 4;
        if (similarityThreshold == null) similarityThreshold = 0.7;
    }
}
