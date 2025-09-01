package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для финального, структурированного отчета от ResourceAllocatorAgent.
 * <p>
 * Этот объект агрегирует результаты анализа использования ресурсов и
 * представляет их в удобном для анализа и применения виде.
 */
@Schema(description = "Отчет с рекомендациями по оптимизации ресурсов")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResourceAllocationReport(
        @Schema(description = "Общий вердикт об утилизации ресурсов")
        AnalysisStatus analysisStatus,

        @Schema(description = "Краткое резюме и ключевые выводы от AI-аналитика")
        String summary,

        @Schema(description = "Пиковое потребление CPU в милликорах за период")
        double peakCpuUsage,

        @Schema(description = "Пиковое потребление памяти в мебибайтах за период")
        double peakMemoryUsage,

        @Schema(description = "Предлагаемая новая конфигурация ресурсов в формате YAML")
        String suggestedConfigYaml
) {
    /**
     * Перечисление возможных статусов утилизации ресурсов.
     */
    public enum AnalysisStatus {
        OPTIMAL,
        OVER_PROVISIONED,
        UNDER_PROVISIONED
    }
}
