package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного обнаруженного расхождения между спецификацией и кодом.
 *
 * @param endpoint  Информация об эндпоинте, где обнаружено расхождение.
 * @param driftType Тип расхождения.
 */
@Schema(description = "Одно расхождение между спецификацией и реализацией")
public record SpecDrift(
        EndpointInfo endpoint,
        DriftType driftType
) {
    /**
     * Перечисление типов расхождений.
     */
    public enum DriftType {
        /**
         * Эндпоинт определен в спецификации, но отсутствует в коде.
         */
        MISSING_IN_CODE,
        /**
         * Эндпоинт реализован в коде, но отсутствует в спецификации (Shadow API).
         */
        MISSING_IN_SPEC
    }
}
