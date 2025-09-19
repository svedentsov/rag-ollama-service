package com.example.ragollama.agent.compliance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от Geo-Legal Navigator Agent.
 *
 * @param jurisdiction  Юрисдикция, по которой проводилась проверка.
 * @param overallStatus Общий вердикт.
 * @param summary       Резюме от AI-юриста.
 * @param violations    Список обнаруженных нарушений.
 */
@Schema(description = "Отчет о проверке на соответствие гео-специфичным нормам")
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoLegalReport(
        String jurisdiction,
        String overallStatus,
        String summary,
        List<GeoLegalViolation> violations
) {
}