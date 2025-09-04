package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального, структурированного отчета от CrossValidatorAgent.
 * <p>Этот объект агрегирует результаты сравнения "доказательств" из различных
 * источников знаний и представляет их в удобном для анализа виде.
 */
@Schema(description = "Отчет о проверке консистентности между источниками знаний")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsistencyReport(
        @Schema(description = "Вердикт: являются ли данные консистентными")
        boolean isConsistent,

        @Schema(description = "Краткое резюме от AI-аналитика, объясняющее вердикт")
        String summary,

        @Schema(description = "Список всех найденных несоответствий и пробелов в знаниях")
        List<Finding> findings
) {

    /**
     * DTO для представления одного конкретного несоответствия.
     *
     * @param findingType        Тип проблемы: противоречие или пробел в знаниях.
     * @param description        Подробное описание проблемы, сгенерированное AI.
     * @param conflictingSources Список имен источников, между которыми обнаружена проблема.
     * @param recommendation     Предлагаемое действие для устранения несоответствия.
     */
    @Schema(description = "Одно найденное несоответствие или пробел в знаниях")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Finding(
            FindingType findingType,
            String description,
            List<String> conflictingSources,
            String recommendation
    ) {
    }

    /**
     * Тип обнаруженной проблемы.
     */
    @Schema(description = "Тип проблемы консистентности")
    public enum FindingType {
        /**
         * Прямое противоречие между двумя или более источниками.
         */
        CONTRADICTION,
        /**
         * Важная информация присутствует в одном источнике, но отсутствует в другом.
         */
        KNOWLEDGE_GAP
    }
}
