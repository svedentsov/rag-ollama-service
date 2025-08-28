package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для структурированного результата анализа пользовательского фидбэка.
 *
 * @param reasoning            Объяснение от AI, почему исходный ответ был неверным.
 * @param correctedDocumentIds Список ID документов, которые должны были быть использованы.
 */
@Schema(description = "Результат анализа пользовательского фидбэка")
@JsonIgnoreProperties(ignoreUnknown = true)
public record FeedbackAnalysisResult(
        String reasoning,
        List<String> correctedDocumentIds
) {
}
