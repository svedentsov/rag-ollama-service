package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного предложенного исправления кода.
 * <p>
 * Этот объект является "продуктом" работы агента по исправлению кода.
 *
 * @param originalCodeSnippet Исходный фрагмент кода, содержащий проблему.
 * @param suggestedFix        Предложенный исправленный фрагмент кода.
 * @param justification       Объяснение, почему предложенное исправление лучше.
 */
@Schema(description = "Предложенное исправление для фрагмента кода")
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodePatch(
        String originalCodeSnippet,
        String suggestedFix,
        String justification
) {
}
