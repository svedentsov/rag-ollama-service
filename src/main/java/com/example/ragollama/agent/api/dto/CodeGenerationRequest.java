package com.example.ragollama.agent.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для запроса к агенту кодогенерации.
 *
 * @param instruction Описание задачи для LLM в свободной форме.
 *                    (например, "Сгенерируй RestAssured тест для проверки статуса 200").
 * @param context     Предоставленный контекст (например, фрагмент OpenAPI спецификации,
 *                    требования), который поможет LLM выполнить задачу.
 */
@Schema(description = "DTO для запроса на генерацию кода")
public record CodeGenerationRequest(
        @Schema(description = "Инструкция для LLM", requiredMode = Schema.RequiredMode.REQUIRED, example = "Сгенерируй RestAssured тест для проверки статуса 200")
        @NotBlank @Size(max = 2048)
        String instruction,

        @Schema(description = "Контекст (спецификация, требования)", requiredMode = Schema.RequiredMode.REQUIRED, example = "GET /api/users/{id} returns UserDTO...")
        @NotBlank @Size(max = 8192)
        String context
) {
}
