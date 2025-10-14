package com.example.ragollama.rag.api.dto;

import com.example.ragollama.rag.domain.model.QueryFormationStep;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Запечатанный (sealed) интерфейс для представления различных частей структурированного
 * потокового ответа (Server-Sent Events).
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StreamingResponsePart.Content.class, name = "content"),
        @JsonSubTypes.Type(value = StreamingResponsePart.Sources.class, name = "sources"),
        @JsonSubTypes.Type(value = StreamingResponsePart.Done.class, name = "done"),
        @JsonSubTypes.Type(value = StreamingResponsePart.Error.class, name = "error")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface StreamingResponsePart {

    /**
     * Представляет фрагмент сгенерированного текстового контента.
     *
     * @param text Часть ответа от LLM.
     */
    @Schema(description = "Часть сгенерированного текстового контента")
    record Content(String text) implements StreamingResponsePart {
    }

    /**
     * Представляет список документов-источников, историю формирования запроса и финальный промпт.
     *
     * @param sources               Список структурированных цитат.
     * @param queryFormationHistory История обработки запроса.
     * @param finalPrompt           Полный текст промпта, отправленного в LLM.
     */
    @Schema(description = "Список источников, история запроса и финальный промпт")
    record Sources(List<SourceCitation> sources, List<QueryFormationStep> queryFormationHistory,
                   String finalPrompt) implements StreamingResponsePart {
    }

    /**
     * Сигнализирует об успешном завершении потока.
     *
     * @param message Сообщение о статусе завершения.
     */
    @Schema(description = "Сигнал об успешном завершении потока")
    record Done(String message) implements StreamingResponsePart {
    }

    /**
     * Сообщает об ошибке, возникшей во время обработки потока.
     *
     * @param message Детальное описание ошибки.
     */
    @Schema(description = "Сообщение об ошибке в потоке")
    record Error(String message) implements StreamingResponsePart {
    }
}
