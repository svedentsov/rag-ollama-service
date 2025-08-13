package com.example.ragollama.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для ответа на RAG-запрос.
 *
 * @param answer          Финальный ответ, сгенерированный LLM на основе найденного контекста и вопроса пользователя.
 * @param sourceCitations Список источников (например, имена файлов), которые были использованы
 *                        для формирования контекста. Это обеспечивает прозрачность и позволяет пользователю проверить исходную информацию.
 */
@Schema(description = "DTO ответа на RAG-запрос")
public record RagQueryResponse(
        @Schema(description = "Сгенерированный ответ", example = "Spring Boot — это фреймворк, который упрощает создание автономных приложений...")
        String answer,

        @Schema(description = "Список источников, использованных для ответа", example = "[\"spring-ai-doc.txt\"]")
        List<String> sourceCitations
) {
}
