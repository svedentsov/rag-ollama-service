package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного конкретного пункта в отчете-ревью.
 *
 * @param category       Категория фидбэка (например, "Читаемость", "Корректность").
 * @param principle      Конкретный принцип или лучшая практика (например, "BDD Naming", "SOLID").
 * @param explanation    Объяснение от AI, в чем заключается суть замечания или похвалы.
 * @param codeSnippet    Фрагмент исходного кода, к которому относится замечание.
 * @param recommendation Конкретное предложение по улучшению (если применимо).
 */
@Schema(description = "Один пункт в отчете от AI-наставника")
@JsonIgnoreProperties(ignoreUnknown = true)
public record MentorshipFinding(
        String category,
        String principle,
        String explanation,
        String codeSnippet,
        String recommendation
) {
}
