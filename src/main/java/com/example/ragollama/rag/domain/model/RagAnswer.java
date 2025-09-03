package com.example.ragollama.rag.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Инкапсулирует результат работы RAG-сервиса.
 * <p>
 * Этот record является "чистым" доменным объектом, который не содержит
 * информации о деталях транспортного уровня или сессий (таких как sessionId).
 * Он представляет собой ядро ответа, сгенерированного RAG-конвейером.
 *
 * @param answer          Финальный ответ, сгенерированный LLM на основе найденного контекста.
 * @param sourceCitations Список структурированных цитат, которые были использованы
 *                        для формирования контекста.
 */
@Schema(description = "Результат работы RAG-сервиса")
public record RagAnswer(
        @Schema(description = "Сгенерированный ответ", example = "Spring Boot — это фреймворк...")
        String answer,

        @Schema(description = "Список структурированных цитат, использованных для ответа")
        List<SourceCitation> sourceCitations
) {
}
