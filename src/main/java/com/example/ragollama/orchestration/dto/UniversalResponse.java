package com.example.ragollama.orchestration.dto;

import com.example.ragollama.agent.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.agent.codegeneration.api.dto.CodeGenerationResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Запечатанный (sealed) интерфейс для представления различных частей
 * унифицированного потокового ответа от оркестратора.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UniversalResponse.TaskStarted.class, name = "task_started"),
        @JsonSubTypes.Type(value = UniversalResponse.Content.class, name = "content"),
        @JsonSubTypes.Type(value = UniversalResponse.Sources.class, name = "sources"),
        @JsonSubTypes.Type(value = UniversalResponse.Code.class, name = "code"),
        @JsonSubTypes.Type(value = UniversalResponse.Done.class, name = "done"),
        @JsonSubTypes.Type(value = UniversalResponse.Error.class, name = "error"),
        @JsonSubTypes.Type(value = UniversalResponse.BugAnalysis.class, name = "bugAnalysis")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface UniversalResponse {

    @Schema(description = "Сигнал о старте задачи, содержит ее ID")
    record TaskStarted(UUID taskId) implements UniversalResponse {
    }

    @Schema(description = "Часть сгенерированного текстового контента")
    record Content(String text) implements UniversalResponse {
    }

    @Schema(description = "Список структурированных цитат, использованных для RAG-ответа")
    record Sources(List<SourceCitation> sources) implements UniversalResponse {
    }

    @Schema(description = "Результат работы агента кодогенерации")
    record Code(String generatedCode, String language) implements UniversalResponse {
    }

    @Schema(description = "Результат работы агента анализа багов")
    record BugAnalysis(BugAnalysisResponse analysis) implements UniversalResponse {
    }

    @Schema(description = "Сигнал об успешном завершении потока")
    record Done(String message) implements UniversalResponse {
    }

    @Schema(description = "Сообщение об ошибке в потоке")
    record Error(String message) implements UniversalResponse {
    }

    /**
     * Фабричный метод для преобразования {@link StreamingResponsePart} от RAG-сервиса.
     */
    static UniversalResponse from(StreamingResponsePart part) {
        return switch (part) {
            case StreamingResponsePart.Content c -> new Content(c.text());
            case StreamingResponsePart.Sources s -> new Sources(s.sources());
            case StreamingResponsePart.Done d -> new Done(d.message());
            case StreamingResponsePart.Error e -> new Error(e.message());
        };
    }

    /**
     * Фабричный метод для преобразования текстового чанка от Чат-сервиса.
     */
    static UniversalResponse from(String textChunk) {
        return new Content(textChunk);
    }

    /**
     * Фабричный метод для преобразования ответа от сервиса кодогенерации.
     */
    static UniversalResponse from(CodeGenerationResponse response) {
        return new Code(response.generatedCode(), response.language());
    }

    /**
     * Фабричный метод для преобразования ответа от сервиса анализа багов.
     */
    static UniversalResponse from(BugAnalysisResponse response) {
        return new BugAnalysis(response);
    }
}
