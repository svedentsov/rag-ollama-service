package com.example.ragollama.agent.openapi.api.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

/**
 * Запечатанный (sealed) интерфейс, представляющий полиморфный источник OpenAPI спецификации.
 *
 * <p>Эта архитектура является эталонной, так как она делает контракт API явным
 * и типобезопасным, полностью устраняя неоднозначность между передачей
 * URL или контента. Jackson автоматически десериализует JSON в правильную
 * реализацию на основе поля `type`.
 *
 * <p><b>ИСПРАВЛЕНО:</b> Добавлена аннотация `@Schema(oneOf = ...)` для корректной
 * генерации OpenAPI-документации для полиморфных типов, а также
 * предоставлены явные, рабочие примеры в каждой реализации.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpenApiSourceRequest.ByUrl.class, name = "url"),
        @JsonSubTypes.Type(value = OpenApiSourceRequest.ByContent.class, name = "content")
})
@Schema(
        description = "Источник OpenAPI спецификации (полиморфный). Укажите одно из: 'url' или 'content'.",
        oneOf = {OpenApiSourceRequest.ByUrl.class, OpenApiSourceRequest.ByContent.class}
)
public sealed interface OpenApiSourceRequest {

    /**
     * Реализация для источника, предоставленного по URL.
     *
     * @param value URL, по которому можно загрузить спецификацию.
     */
    @Schema(description = "Источник спецификации по URL.",
            example = "{\"type\": \"url\", \"value\": \"https://petstore3.swagger.io/api/v3/openapi.json\"}")
    record ByUrl(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @URL
            String value
    ) implements OpenApiSourceRequest {
    }

    /**
     * Реализация для источника, предоставленного в виде текстового контента.
     *
     * @param value Содержимое спецификации (YAML или JSON).
     */
    @Schema(description = "Источник спецификации в виде текста.",
            example = "{\"type\": \"content\", \"value\": \"openapi: 3.0.0...\"}")
    record ByContent(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank
            String value
    ) implements OpenApiSourceRequest {
    }
}
