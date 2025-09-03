package com.example.ragollama.rag.api.dto;

import com.example.ragollama.rag.domain.model.SourceCitation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Запечатанный (sealed) интерфейс для представления различных частей структурированного
 * потокового ответа (Server-Sent Events).
 * <p>
 * Использование запечатанного интерфейса с аннотациями Jackson позволяет
 * передавать по одному потоку события разного типа (текст, метаданные, сигналы),
 * что делает API более надежным и расширяемым по сравнению с передачей "сырого" текста.
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
     * Представляет список документов-источников, использованных для ответа.
     * Обычно отправляется один раз в конце потока.
     *
     * @param sources Список структурированных цитат.
     */
    @Schema(description = "Список структурированных цитат, использованных для ответа")
    record Sources(List<SourceCitation> sources) implements StreamingResponsePart {
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
