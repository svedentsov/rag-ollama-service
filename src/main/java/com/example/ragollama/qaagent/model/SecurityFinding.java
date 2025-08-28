package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одной конкретной проблемы безопасности, обнаруженной любым из сканеров.
 *
 * @param findingId      Уникальный идентификатор находки.
 * @param source         Источник обнаружения (SAST, DAST, LOGS).
 * @param severity       Серьезность ("Critical", "High", "Medium", "Low").
 * @param description    Подробное описание уязвимости.
 * @param location       Местоположение (например, путь к файлу и строка).
 * @param recommendation Конкретная рекомендация по исправлению.
 */
@Schema(description = "Стандартизированное описание одной найденной уязвимости")
@JsonIgnoreProperties(ignoreUnknown = true)
public record SecurityFinding(
        String findingId,
        String source,
        String severity,
        String description,
        String location,
        String recommendation
) {
}
