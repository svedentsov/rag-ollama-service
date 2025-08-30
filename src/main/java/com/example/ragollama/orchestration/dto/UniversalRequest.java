package com.example.ragollama.orchestration.dto;

import com.example.ragollama.agent.codegeneration.api.dto.CodeGenerationRequest;
import com.example.ragollama.chat.api.dto.ChatRequest;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * Универсальный DTO для всех входящих запросов к оркестратору.
 * <p>
 * Эта версия использует вложенный {@link RagOptions} и каскадную валидацию
 * для обеспечения надежности и чистоты API. Аннотации {@code @Schema}
 * дополнены корректными примерами для генерации правильной документации.
 *
 * @param query      Основной текстовый запрос или инструкция от пользователя.
 * @param sessionId  Опциональный идентификатор сессии.
 * @param context    Опциональный дополнительный контекст (например, для кодогенерации).
 * @param ragOptions Опциональные, но валидируемые параметры для RAG-поиска.
 */
@Schema(description = "Универсальный DTO для запросов к AI-оркестратору")
public record UniversalRequest(
        @Schema(description = "Основной запрос или инструкция от пользователя", requiredMode = Schema.RequiredMode.REQUIRED, example = "Могу ли я получить деньги за неиспользованный отпуск?")
        @NotBlank @Size(max = 4096)
        String query,

        @Schema(description = "Опциональный ID сессии для продолжения диалога", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID sessionId,

        @Schema(description = "Опциональный дополнительный контекст (например, для генерации кода)", example = "string")
        String context,

        @Schema(description = "Опциональные параметры для RAG-поиска")
        @Valid // Включает каскадную валидацию для вложенного объекта
        RagOptions ragOptions
) {
    /**
     * Вложенный record для инкапсуляции и валидации RAG-параметров.
     */
    @Schema(description = "Параметры для RAG-поиска")
    public record RagOptions(
            @Schema(description = "Количество извлекаемых чанков", defaultValue = "4", example = "3")
            @Min(value = 1, message = "topK должен быть не меньше 1")
            @Max(value = 10, message = "topK не должен превышать 10")
            Integer topK,

            @Schema(description = "Порог схожести (0.1-1.0)", defaultValue = "0.7", example = "0.75")
            @DecimalMin(value = "0.1", message = "similarityThreshold должен быть не меньше 0.1")
            @Max(value = 1, message = "similarityThreshold не должен превышать 1.0")
            Double similarityThreshold
    ) {
    }

    /**
     * Преобразует универсальный запрос в специфичный DTO для RAG-сервиса.
     *
     * @return Экземпляр {@link RagQueryRequest}.
     */
    public RagQueryRequest toRagQueryRequest() {
        Integer topK = (this.ragOptions != null) ? this.ragOptions.topK() : null;
        Double similarityThreshold = (this.ragOptions != null) ? this.ragOptions.similarityThreshold() : null;
        return new RagQueryRequest(this.query, this.sessionId, topK, similarityThreshold);
    }

    /**
     * Преобразует универсальный запрос в специфичный DTO для Чат-сервиса.
     *
     * @return Экземпляр {@link ChatRequest}.
     */
    public ChatRequest toChatRequest() {
        return new ChatRequest(this.query, this.sessionId);
    }

    /**
     * Преобразует универсальный запрос в специфичный DTO для сервиса кодогенерации.
     *
     * @return Экземпляр {@link CodeGenerationRequest}.
     */
    public CodeGenerationRequest toCodeGenerationRequest() {
        return new CodeGenerationRequest(this.query, this.context != null ? this.context : "");
    }
}
