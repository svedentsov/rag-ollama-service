package com.example.ragollama.agent.codegeneration.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO, содержащий результат работы агента кодогенерации.
 *
 * @param generatedCode Сгенерированный фрагмент кода.
 * @param language      Язык программирования, на котором написан код (например, "java").
 */
@Schema(description = "DTO с результатом генерации кода")
public record CodeGenerationResponse(
        @Schema(description = "Сгенерированный код")
        String generatedCode,
        @Schema(description = "Язык программирования", example = "java")
        String language
) {
}
