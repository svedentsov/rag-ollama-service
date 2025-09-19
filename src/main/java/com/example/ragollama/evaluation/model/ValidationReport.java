package com.example.ragollama.evaluation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для структурированного отчета от AI-критика (ResponseValidator).
 *
 * @param isValid  Финальный вердикт: прошел ли ответ проверку.
 * @param findings Список найденных проблем (если isValid = false).
 */
@Schema(description = "Отчет о валидации сгенерированного RAG-ответа")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ValidationReport(
        @Schema(description = "Финальный вердикт: прошел ли ответ проверку")
        boolean isValid,

        @Schema(description = "Список найденных проблем (если isValid = false)")
        List<ValidationFinding> findings
) {
    /**
     * DTO для описания одной конкретной проблемы в ответе.
     *
     * @param type        Тип проблемы (галлюцинация, неполнота, некорректная цитата).
     * @param description Подробное описание проблемы.
     */
    @Schema(description = "Описание одной найденной проблемы в ответе")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidationFinding(
            @Schema(description = "Тип проблемы")
            FindingType type,

            @Schema(description = "Подробное описание проблемы")
            String description
    ) {
    }

    /**
     * Тип обнаруженной проблемы.
     */
    public enum FindingType {
        /**
         * Утверждение в ответе не подтверждено предоставленными источниками.
         */
        HALLUCINATION,
        /**
         * Ответ не полностью раскрывает исходный вопрос пользователя.
         */
        INCOMPLETE_ANSWER,
        /**
         * Цитата указывает на источник, который не содержит релевантной информации.
         */
        INCORRECT_CITATION
    }
}