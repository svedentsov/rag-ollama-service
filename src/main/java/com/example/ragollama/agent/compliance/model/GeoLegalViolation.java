package com.example.ragollama.agent.compliance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для описания одного нарушения гео-специфичных норм.
 *
 * @param articleViolated Статья закона, которая была нарушена (например, "GDPR Article 32").
 * @param description     Описание нарушения.
 * @param location        Место в коде, где найдено нарушение.
 * @param recommendation  Рекомендация по исправлению.
 */
@Schema(description = "Описание одного нарушения гео-специфичных норм")
@JsonIgnoreProperties(ignoreUnknown = true)
record GeoLegalViolation(
        String articleViolated,
        String description,
        String location,
        String recommendation
) {
}