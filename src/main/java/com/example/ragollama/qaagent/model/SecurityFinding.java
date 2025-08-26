package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одной конкретной проблемы безопасности.
 *
 * @param filePath       Путь к файлу, содержащему уязвимость.
 * @param lines          Диапазон строк, где обнаружена проблема.
 * @param riskCategory   Категория риска (например, "PII Exposure").
 * @param severity       Серьезность ("Critical", "High", "Medium", "Low").
 * @param description    Подробное описание проблемы.
 * @param recommendation Конкретная рекомендация по исправлению, возможно, с примером кода.
 */
@Schema(description = "Описание одной найденной проблемы безопасности")
@JsonIgnoreProperties(ignoreUnknown = true)
public record SecurityFinding(
        String filePath,
        String lines,
        String riskCategory,
        String severity,
        String description,
        String recommendation
) {
}
