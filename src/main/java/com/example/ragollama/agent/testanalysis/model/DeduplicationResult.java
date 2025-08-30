package com.example.ragollama.agent.testanalysis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного подтвержденного дубликата тест-кейса.
 * <p>
 * Этот объект является результатом работы агента-дедупликатора.
 *
 * @param duplicateSourceId Идентификатор (например, путь к файлу) найденного дубликата.
 * @param justification     Объяснение от LLM, почему этот тест считается дубликатом.
 * @param similarityScore   Оценка семантической схожести от векторного поиска (если доступна).
 */
@Schema(description = "Структурированное описание найденного дубликата тест-кейса")
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeduplicationResult(
        @Schema(description = "Идентификатор найденного дубликата")
        String duplicateSourceId,

        @Schema(description = "Объяснение от LLM, почему тест является дубликатом")
        String justification,

        @Schema(description = "Оценка семантической схожести от векторного поиска")
        Float similarityScore
) {
}
