package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для описания одного изменения в контракте данных.
 *
 * @param changeType Тип изменения (FIELD_REMOVED, FIELD_TYPE_CHANGED, FIELD_ADDED).
 * @param fieldName  Имя затронутого поля.
 * @param oldValue   Старое значение/тип.
 * @param newValue   Новое значение/тип.
 * @param breaking   Является ли изменение ломающим.
 */
@Schema(description = "Описание одного изменения в контракте")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContractChange(
        String changeType,
        String fieldName,
        String oldValue,
        String newValue,
        boolean breaking
) {
}
