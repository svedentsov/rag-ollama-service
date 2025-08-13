package com.example.ragollama.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO для ответа на RAG-запрос.
 *
 * @param answer          Ответ, сгенерированный LLM на основе найденного контекста.
 * @param sourceCitations Список источников, использованных для генерации ответа.
 */
@Schema(description = "DTO ответа на RAG-запрос")
public record RagQueryResponse(
        @Schema(description = "Сгенерированный ответ")
        String answer,
        @Schema(description = "Список источников, использованных для ответа")
        List<String> sourceCitations
) {
}
