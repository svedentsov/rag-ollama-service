package com.example.ragollama.agent.compliance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного нарушения лицензионной политики.
 *
 * @param dependency         Зависимость, нарушившая политику.
 * @param violatedPolicyRule Правило из политики, которое было нарушено.
 * @param reason             Объяснение от AI, почему это является нарушением.
 */
@Schema(description = "Одно нарушение лицензионной политики")
@JsonIgnoreProperties(ignoreUnknown = true)
public record LicenseViolation(
        ScannedDependency dependency,
        String violatedPolicyRule,
        String reason
) {
}
