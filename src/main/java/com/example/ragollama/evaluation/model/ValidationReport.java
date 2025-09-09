package com.example.ragollama.evaluation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Отчет о валидации сгенерированного RAG-ответа")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ValidationReport(
        @Schema(description = "Финальный вердикт: прошел ли ответ проверку")
        boolean isValid,

        @Schema(description = "Список найденных проблем (если isValid = false)")
        List<ValidationFinding> findings
) {
    @Schema(description = "Описание одной найденной проблемы в ответе")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidationFinding(
            @Schema(description = "Тип проблемы")
            FindingType type,

            @Schema(description = "Подробное описание проблемы")
            String description
    ) {
    }

    public enum FindingType {
        HALLUCINATION,      // Утверждение не подтверждено источниками
        INCOMPLETE_ANSWER,  // Ответ неполный
        INCORRECT_CITATION  // Цитата указывает на нерелевантный источник
    }
}
