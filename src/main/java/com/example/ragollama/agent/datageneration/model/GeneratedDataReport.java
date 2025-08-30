package com.example.ragollama.agent.datageneration.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * DTO для финального отчета от агента генерации данных.
 *
 * @param rowCount      Количество сгенерированных строк.
 * @param sourceProfile Статистический профиль, на основе которого велась генерация.
 * @param syntheticData Сгенерированный набор данных.
 */
@Schema(description = "Отчет о генерации статистически-релевантных синтетических данных")
public record GeneratedDataReport(
        int rowCount,
        DataProfile sourceProfile,
        List<Map<String, Object>> syntheticData
) {
}
