package com.example.ragollama.agent.datageneration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * DTO для результата работы агента по созданию подмножества данных.
 *
 * @param generatedSql SQL-запрос, сгенерированный LLM для выборки данных.
 * @param maskedData   Список карт, где каждая карта представляет одну строку
 *                     из базы данных с замаскированными PII.
 * @param rowsSelected Количество строк, выбранных из БД до маскирования.
 * @param rowsReturned Количество строк, возвращенных в ответе.
 */
@Schema(description = "Результат работы агента по созданию подмножества данных")
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataSubsetResult(
        @Schema(description = "SQL-запрос, сгенерированный AI для выборки данных")
        String generatedSql,
        @Schema(description = "Количество выбранных строк")
        int rowsSelected,
        @Schema(description = "Количество возвращенных строк (может быть меньше из-за лимита)")
        int rowsReturned,
        @Schema(description = "Подмножество данных с замаскированными PII")
        List<Map<String, Object>> maskedData
) {
}
