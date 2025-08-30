package com.example.ragollama.agent.compliance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от агента проверки на соответствие политикам конфиденциальности.
 *
 * @param overallStatus Общий вердикт: COMPLIANT или NON_COMPLIANT.
 * @param summary       Резюме от AI-аналитика.
 * @param violations    Список обнаруженных нарушений.
 */
@Schema(description = "Отчет о проверке на соответствие политикам конфиденциальности")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PrivacyReport(
        String overallStatus,
        String summary,
        List<PrivacyViolation> violations
) {
}
