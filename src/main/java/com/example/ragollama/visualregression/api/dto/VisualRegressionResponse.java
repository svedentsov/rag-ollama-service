package com.example.ragollama.visualregression.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для ответа от агента визуального анализа.
 *
 * @param differences Список найденных отличий, описанных естественным языком.
 * @param passed      Флаг, указывающий, пройдена ли проверка (true, если отличий не найдено).
 */
@Schema(description = "Результат сравнения двух изображений")
public record VisualRegressionResponse(
        @Schema(description = "Список визуальных отличий, описанных AI")
        List<String> differences,
        @Schema(description = "Пройдена ли проверка (true, если отличий нет)")
        boolean passed
) {
}
