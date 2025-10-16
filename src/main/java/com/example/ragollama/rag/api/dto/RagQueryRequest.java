package com.example.ragollama.rag.api.dto;

import com.example.ragollama.orchestration.dto.UniversalRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "DTO для RAG-запроса с поддержкой сессий и файлов")
public record RagQueryRequest(
        @Schema(description = "Вопрос пользователя", requiredMode = Schema.RequiredMode.REQUIRED, example = "Что такое Spring Boot?")
        @NotBlank(message = "Запрос не может быть пустым")
        @Size(max = 2048, message = "Запрос не должен превышать 2048 символов")
        String query,

        @Schema(description = "Опциональный ID сессии для продолжения диалога")
        UUID sessionId,

        @Schema(description = "Количество извлекаемых чанков", defaultValue = "4")
        Integer topK,

        @Schema(description = "Порог схожести (0.1-1.0)", defaultValue = "0.7")
        Double similarityThreshold,

        @Schema(description = "Опциональный список ID файлов для использования в качестве контекста")
        List<UUID> fileIds
) {
    public RagQueryRequest {
        if (topK == null) topK = 4;
        if (similarityThreshold == null) similarityThreshold = 0.7;
    }

    public UniversalRequest toUniversalRequest() {
        UniversalRequest.RagOptions ragOptions = new UniversalRequest.RagOptions(this.topK, this.similarityThreshold);
        return new UniversalRequest(this.query, this.sessionId, null, null, ragOptions, null, this.fileIds);
    }
}
