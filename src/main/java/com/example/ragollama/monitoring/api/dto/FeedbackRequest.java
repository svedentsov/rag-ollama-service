package com.example.ragollama.monitoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO для запроса на отправку обратной связи.
 *
 * @param requestId Уникальный идентификатор запроса (полученный из заголовка X-Request-ID),
 *                  на который дается обратная связь.
 * @param isHelpful Оценка ответа: {@code true} - полезный, {@code false} - не полезный.
 * @param comment   Опциональный текстовый комментарий от пользователя.
 */
@Schema(description = "DTO для отправки обратной связи по RAG-ответу")
public record FeedbackRequest(
        @Schema(description = "ID запроса из заголовка X-Request-ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String requestId,

        @Schema(description = "Был ли ответ полезен?", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean isHelpful,

        @Schema(description = "Опциональный комментарий пользователя")
        @Size(max = 2048)
        String comment
) {
}
