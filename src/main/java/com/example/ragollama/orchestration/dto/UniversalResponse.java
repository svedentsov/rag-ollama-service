package com.example.ragollama.orchestration.dto;

import com.example.ragollama.agent.api.dto.CodeGenerationResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Запечатанный (sealed) интерфейс для представления различных частей
 * унифицированного потокового ответа от оркестратора.
 * <p>
 * Позволяет передавать по одному SSE-потоку события разного типа,
 * инкапсулируя ответы от всех нижележащих сервисов.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UniversalResponse.Content.class, name = "content"),
        @JsonSubTypes.Type(value = UniversalResponse.Sources.class, name = "sources"),
        @JsonSubTypes.Type(value = UniversalResponse.Code.class, name = "code"),
        @JsonSubTypes.Type(value = UniversalResponse.Done.class, name = "done"),
        @JsonSubTypes.Type(value = UniversalResponse.Error.class, name = "error")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface UniversalResponse {

    @Schema(description = "Часть сгенерированного текстового контента")
    record Content(String text) implements UniversalResponse {
    }

    @Schema(description = "Список документов-источников, использованных для RAG-ответа")
    record Sources(List<String> sources) implements UniversalResponse {
    }

    @Schema(description = "Результат работы агента кодогенерации")
    record Code(String generatedCode, String language) implements UniversalResponse {
    }

    @Schema(description = "Сигнал об успешном завершении потока")
    record Done(String message) implements UniversalResponse {
    }

    @Schema(description = "Сообщение об ошибке в потоке")
    record Error(String message) implements UniversalResponse {
    }

    /**
     * Фабричный метод для преобразования {@link StreamingResponsePart} от RAG-сервиса.
     *
     * @param part Часть потокового ответа от RAG.
     * @return Соответствующий экземпляр {@link UniversalResponse}.
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
     *
     * @param textChunk Часть текстового ответа.
     * @return Экземпляр {@link Content}.
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
}
