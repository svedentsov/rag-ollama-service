package com.example.ragollama.agent.testanalysis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одной связи в матрице трассируемости.
 *
 * @param testName           Имя сгенерированного теста.
 * @param coveredRequirement Фрагмент требования, который проверяется этим тестом.
 * @param explanation        Объяснение, как именно тест проверяет это требование.
 */
@Schema(description = "Связь между тестом и требованием")
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraceabilityLink(
        String testName,
        String coveredRequirement,
        String explanation
) {
}
