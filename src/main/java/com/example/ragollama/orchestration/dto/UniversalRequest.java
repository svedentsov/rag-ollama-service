package com.example.ragollama.orchestration.dto;

import com.example.ragollama.agent.api.dto.CodeGenerationRequest;
import com.example.ragollama.chat.api.dto.ChatRequest;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Универсальный DTO для всех входящих запросов к оркестратору.
 * <p>
 * Этот объект агрегирует все возможные поля, которые могут потребоваться
 * различным сервисам (RAG, Chat, Code Generation), и предоставляет
 * методы для преобразования в специфичные DTO.
 *
 * @param query               Основной текстовый запрос или инструкция от пользователя.
 * @param sessionId           Опциональный идентификатор сессии для поддержания контекста.
 * @param context             Опциональный дополнительный контекст (например, для кодогенерации).
 * @param topK                Параметр для RAG-поиска: количество извлекаемых чанков.
 * @param similarityThreshold Параметр для RAG-поиска: порог схожести.
 */
@Schema(description = "Универсальный DTO для запросов к AI-оркестратору")
public record UniversalRequest(
        @Schema(description = "Основной запрос или инструкция от пользователя", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 4096)
        String query,

        @Schema(description = "Опциональный ID сессии для продолжения диалога")
        UUID sessionId,

        @Schema(description = "Опциональный дополнительный контекст (например, для генерации кода)")
        String context,

        @Schema(description = "Количество извлекаемых чанков (для RAG)")
        Integer topK,

        @Schema(description = "Порог схожести (для RAG)")
        Double similarityThreshold
) {
    /**
     * Преобразует универсальный запрос в специфичный DTO для RAG-сервиса.
     *
     * @return Экземпляр {@link RagQueryRequest}.
     */
    public RagQueryRequest toRagQueryRequest() {
        return new RagQueryRequest(this.query, this.sessionId, this.topK, this.similarityThreshold);
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
