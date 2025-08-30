package com.example.ragollama.agent.accessibility.api.dto;

import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для ответа от эндпоинта аудита доступности.
 * <p>
 * Этот record является публичным контрактом API и скрывает внутреннюю
 * структуру {@link com.example.ragollama.agent.AgentResult}.
 *
 * @param report Финальный отчет об аудите, который будет виден клиенту.
 */
@Schema(description = "DTO ответа с результатами аудита доступности")
public record AccessibilityAuditResponse(
        AccessibilityReport report
) {
}
