package com.example.ragollama.agent.datacontract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для структурированного результата проверки контракта данных.
 *
 * @param validationStatus Вердикт: COMPATIBLE, BREAKING_CHANGE, или EXTENSION.
 * @param changes          Список обнаруженных изменений.
 * @param recommendations  Рекомендации по миграции для потребителей.
 */
@Schema(description = "Результат проверки контракта данных")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContractValidationResult(
        @Schema(description = "Вердикт о совместимости")
        ValidationStatus validationStatus,
        @Schema(description = "Список обнаруженных изменений")
        List<ContractChange> changes,
        @Schema(description = "Рекомендации для потребителей")
        String recommendations
) {
    /**
     * Статус совместимости.
     */
    public enum ValidationStatus {
        COMPATIBLE, BREAKING_CHANGE, EXTENSION
    }
}
