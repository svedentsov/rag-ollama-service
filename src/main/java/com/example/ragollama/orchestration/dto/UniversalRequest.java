package com.example.ragollama.orchestration.dto;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.codegeneration.api.dto.CodeGenerationRequest;
import com.example.ragollama.agent.openapi.api.dto.OpenApiSourceRequest;
import com.example.ragollama.chat.api.dto.ChatRequest;
import com.example.ragollama.orchestration.validation.ValidRequestPayload;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Универсальный DTO для всех входящих запросов к оркестратору.
 * Валидируется с помощью кастомной аннотации {@link ValidRequestPayload}.
 *
 * @param query         Основной текстовый запрос или инструкция от пользователя.
 * @param sessionId     Опциональный идентификатор сессии.
 * @param context       Опциональный дополнительный контекст (например, содержимое файла).
 * @param history       Опциональная история сообщений для сохранения контекста при регенерации.
 * @param ragOptions    Опциональные, но валидируемые параметры для RAG-поиска.
 * @param openApiSource Опциональный источник OpenAPI спецификации для анализа.
 * @param fileIds       // <-- НОВОЕ ПОЛЕ
 *                      Опциональный список ID файлов из файлового менеджера для использования в качестве контекста.
 */
@Schema(description = "Универсальный DTO для запросов к AI-оркестратору")
@ValidRequestPayload
public record UniversalRequest(
        @Schema(description = "Основной запрос или инструкция от пользователя", example = "Могу ли я получить деньги за неиспользованный отпуск?")
        @Size(max = 4096)
        String query,

        @Schema(description = "Опциональный ID сессии для продолжения диалога", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID sessionId,

        @Schema(description = "Опциональный дополнительный контекст (например, содержимое файла)", example = "string")
        String context,

        @Schema(description = "Опциональная история сообщений для контекста регенерации")
        @Valid
        List<MessageDto> history,

        @Schema(description = "Опциональные параметры для RAG-поиска")
        @Valid
        RagOptions ragOptions,

        @Schema(description = "Опциональный источник OpenAPI спецификации для анализа")
        @Valid
        OpenApiSourceRequest openApiSource,

        @Schema(description = "Опциональный список ID файлов для использования в качестве контекста")
        List<UUID> fileIds
) {

    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска.
     */
    public AgentContext toAgentContext() {
        Map<String, Object> payload = new HashMap<>();
        if (query != null) payload.put("query", query);
        if (sessionId != null) payload.put("sessionId", sessionId);
        if (context != null) payload.put("context", context);
        if (history != null) payload.put("history", history);
        if (ragOptions != null) payload.put("ragOptions", ragOptions);
        if (openApiSource != null) payload.put("source", openApiSource);
        if (fileIds != null) payload.put("fileIds", fileIds); // Добавляем fileIds в контекст
        return new AgentContext(payload);
    }

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
            @DecimalMax(value = "1.0", message = "similarityThreshold не должен превышать 1.0")
            Double similarityThreshold
    ) {
    }

    public RagQueryRequest toRagQueryRequest() {
        Integer topK = (this.ragOptions != null) ? this.ragOptions.topK() : null;
        Double similarityThreshold = (this.ragOptions != null) ? this.ragOptions.similarityThreshold() : null;
        return new RagQueryRequest(this.query, this.sessionId, topK, similarityThreshold, this.fileIds); // <-- Добавляем fileIds
    }

    public ChatRequest toChatRequest() {
        return new ChatRequest(this.query, this.sessionId);
    }

    public CodeGenerationRequest toCodeGenerationRequest() {
        return new CodeGenerationRequest(this.query, this.context != null ? this.context : "");
    }
}
