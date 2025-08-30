package com.example.ragollama.agent.datageneration.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * DTO для финального отчета от агента генерации синтетических данных.
 *
 * @param rowCount       Количество сгенерированных строк.
 * @param sourceProfile  Исходный (неприватный) статистический профиль.
 * @param privateProfile Приватный (зашумленный) статистический профиль.
 * @param syntheticData  Сгенерированный набор данных.
 */
@Schema(description = "Отчет о генерации синтетических данных с дифференциальной приватностью")
public record SyntheticDataReport(
        int rowCount,
        DataProfile sourceProfile,
        DataProfile privateProfile,
        List<Map<String, Object>> syntheticData
) {
}
