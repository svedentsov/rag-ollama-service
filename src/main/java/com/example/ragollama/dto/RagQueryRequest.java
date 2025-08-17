package com.example.ragollama.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * DTO для RAG-запроса, содержащий вопрос пользователя, параметры поиска и опциональный ID сессии.
 *
 * @param query               Вопрос пользователя, на основе которого будет производиться поиск и генерация ответа.
 * @param sessionId           Опциональный идентификатор сессии. Если предоставлен, в промпт будет добавлена
 *                            история диалога для поддержания контекста. Если null, будет создана новая сессия.
 * @param topK                Количество наиболее релевантных документов (чанков) для извлечения из векторного хранилища.
 * @param similarityThreshold Минимальный порог схожести (от 0.0 до 1.0), которому должны соответствовать
 *                            извлекаемые документы. Документы с меньшей схожестью будут отфильтрованы.
 */
@Schema(description = "DTO для RAG-запроса с поддержкой сессий")
public record RagQueryRequest(
        @Schema(description = "Вопрос пользователя", requiredMode = Schema.RequiredMode.REQUIRED, example = "Что такое Spring Boot?")
        @NotBlank(message = "Запрос не может быть пустым")
        @Size(max = 2048, message = "Запрос не должен превышать 2048 символов")
        String query,

        @Schema(description = "Опциональный ID сессии для продолжения диалога", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID sessionId,

        @Schema(description = "Количество извлекаемых чанков", defaultValue = "4", example = "5")
        @NotNull @Min(1) @Max(10)
        Integer topK,

        @Schema(description = "Порог схожести (0.0-1.0)", defaultValue = "0.7", example = "0.75")
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        Double similarityThreshold
) {
    /**
     * Компактный конструктор для установки значений по умолчанию, если они не предоставлены в запросе.
     */
    public RagQueryRequest {
        if (topK == null) topK = 4;
        if (similarityThreshold == null) similarityThreshold = 0.7;
    }
}
