package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для описания одного нарушения политики конфиденциальности.
 *
 * @param piiType        Тип обнаруженных персональных данных (e.g., "Email", "IP Address").
 * @param action         Действие, которое выполняется над данными (e.g., "Logging", "API Call").
 * @param location       Путь к файлу и строка, где обнаружено нарушение.
 * @param description    Объяснение, почему это является нарушением политики.
 * @param recommendation Предложение по исправлению.
 */
@Schema(description = "Описание одного нарушения политики конфиденциальности")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PrivacyViolation(
        String piiType,
        String action,
        String location,
        String description,
        String recommendation
) {
}
