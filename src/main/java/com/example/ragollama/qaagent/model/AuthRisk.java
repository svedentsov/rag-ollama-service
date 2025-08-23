package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO для представления одного риска безопасности, обнаруженного агентом.
 * <p>
 * Спроектирован для десериализации JSON-ответа от LLM.
 *
 * @param riskLevel         Уровень риска (например, "High", "Medium", "Low").
 * @param description       Человекочитаемое описание проблемы.
 * @param recommendation    Конкретное предложение по устранению риска.
 * @param violatesPrinciple Какой принцип безопасности был нарушен (например, "Least Privilege").
 * @param relatedRule       Правило из исходного списка, которое вызвало срабатывание.
 */
@Schema(description = "Структурированное описание одного риска безопасности")
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthRisk(
        String riskLevel,
        String description,
        String recommendation,
        String violatesPrinciple,
        Map<String, String> relatedRule
) {
}
