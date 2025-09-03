package com.example.ragollama.rag.api.dto;

import com.example.ragollama.rag.domain.model.SourceCitation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * DTO для ответа на RAG-запрос.
 *
 * @param answer          Финальный ответ, сгенерированный LLM на основе найденного контекста и вопроса пользователя.
 * @param sourceCitations Список структурированных цитат, которые были использованы
 *                        для формирования контекста.
 * @param sessionId       Идентификатор сессии, к которой относится данный ответ. Клиент должен
 *                        использовать этот ID для последующих запросов, чтобы продолжить диалог.
 */
@Schema(description = "DTO ответа на RAG-запрос")
public record RagQueryResponse(
        @Schema(description = "Сгенерированный ответ", example = "Spring Boot — это фреймворк...")
        String answer,

        @Schema(description = "Список структурированных цитат, использованных для ответа")
        List<SourceCitation> sourceCitations,

        @Schema(description = "ID сессии для продолжения диалога", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID sessionId
) {
}
