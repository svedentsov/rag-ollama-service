package com.example.ragollama.rag.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO для RAG-запроса, содержащий вопрос пользователя, параметры поиска и опциональный ID сессии.
 * <p>
 * Валидация параметров поиска вынесена в {@link com.example.ragollama.orchestration.dto.UniversalRequest},
 * этот DTO теперь является простым контейнером данных.
 *
 * @param query               Вопрос пользователя.
 * @param sessionId           Опциональный идентификатор сессии.
 * @param topK                Количество извлекаемых документов.
 * @param similarityThreshold Порог схожести.
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
        Integer topK,

        @Schema(description = "Порог схожести (0.1-1.0)", defaultValue = "0.7", example = "0.75")
        Double similarityThreshold
) {
    /**
     * Компактный конструктор для установки значений по умолчанию, если они не предоставлены (null).
     */
    public RagQueryRequest {
        if (topK == null) topK = 4;
        if (similarityThreshold == null) similarityThreshold = 0.7;
    }
}
