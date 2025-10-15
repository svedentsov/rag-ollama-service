package com.example.ragollama.orchestration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.messages.MessageType;

/**
 * DTO для представления одного сообщения в истории чата при передаче через API.
 * <p>
 * Этот record является частью публичного контракта и решает проблему
 * десериализации абстрактного типа {@link org.springframework.ai.chat.messages.Message}.
 *
 * @param type    Тип сообщения (USER, ASSISTANT, SYSTEM).
 * @param content Текст сообщения.
 */
@Schema(description = "DTO для одного сообщения в истории чата")
public record MessageDto(
        @Schema(description = "Тип сообщения", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER")
        @NotBlank
        String type,

        @Schema(description = "Текст сообщения", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String content
) {
    /**
     * Конструктор для десериализации JSON.
     * Преобразует строковый 'type' в Enum `MessageType`.
     */
    @JsonCreator
    public MessageDto(@JsonProperty("type") String type, @JsonProperty("content") String content) {
        // Простая валидация, чтобы убедиться, что тип корректен
        this.type = MessageType.valueOf(type.toUpperCase()).getValue();
        this.content = content;
    }
}
