package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO, представляющий план по исправлению сбоя, сгенерированный ErrorHandlerAgent.
 */
@Schema(description = "План по исправлению сбоя, сгенерированный AI")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemediationPlan(
        @Schema(description = "Тип действия, которое необходимо предпринять")
        ActionType action,
        @Schema(description = "Объяснение от AI, почему был выбран этот план")
        String justification,
        @Schema(description = "Карта с новыми или измененными аргументами для действия RETRY_WITH_FIX")
        Map<String, Object> modifiedArguments
) implements Serializable {

    /**
     * Перечисление возможных действий по исправлению.
     */
    public enum ActionType {
        /**
         * Повторить выполнение шага с исправленными аргументами.
         */
        RETRY_WITH_FIX,
        /**
         * Признать невозможность автоматического исправления и завершить конвейер.
         */
        FAIL_GRACEFULLY
    }
}
