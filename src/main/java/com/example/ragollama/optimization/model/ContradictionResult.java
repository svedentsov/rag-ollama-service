package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для структурированного результата анализа двух документов на предмет противоречий.
 *
 * @param isContradictory Вердикт: содержат ли документы противоречивую информацию.
 * @param justification   Подробное объяснение найденного противоречия (если есть).
 */
@Schema(description = "Результат анализа двух документов на предмет противоречий")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContradictionResult(
        @Schema(description = "Вердикт: содержат ли документы противоречивую информацию")
        boolean isContradictory,

        @Schema(description = "Подробное объяснение найденного противоречия (если есть)")
        String justification
) {
}