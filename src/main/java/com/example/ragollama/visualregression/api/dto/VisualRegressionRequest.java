package com.example.ragollama.visualregression.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

/**
 * DTO для запроса на визуальный анализ.
 * <p>
 * Содержит два изображения, закодированных в Base64, которые будут
 * отправлены на сравнение в мультимодальную LLM.
 *
 * @param baselineImageBase64 Эталонное изображение (снимок "до" изменений) в формате Base64.
 * @param currentImageBase64  Текущее изображение (снимок "после" изменений) в формате Base64.
 */
@Schema(description = "DTO для запроса на визуальное регрессионное тестирование")
public record VisualRegressionRequest(
        @Schema(description = "Эталонное изображение в формате Base64", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Length(min = 100)
        String baselineImageBase64,

        @Schema(description = "Текущее изображение в формате Base64", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Length(min = 100)
        String currentImageBase64
) {
}
