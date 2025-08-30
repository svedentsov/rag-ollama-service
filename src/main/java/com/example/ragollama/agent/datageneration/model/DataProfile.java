package com.example.ragollama.agent.datageneration.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO для представления статистического профиля набора данных.
 */
@Schema(description = "Статистический профиль набора данных")
@Data
@Builder
public class DataProfile {
    private long rowCount;
    private Map<String, ColumnProfile> columnProfiles;

    /**
     * Статистический профиль одной колонки.
     */
    @Schema(description = "Статистический профиль одной колонки")
    @Data
    @Builder
    public static class ColumnProfile {
        private String dataType;
        // Для числовых
        private Double min, max, mean, stdDev;
        // Для категориальных
        private Map<String, Long> valueCounts;
        // Общее
        private long nullCount;
    }
}
