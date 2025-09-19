package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от Observability Director Agent.
 *
 * @param summary           Краткое резюме анализа.
 * @param totalDuration     Общее время выполнения запроса.
 * @param bottleneck        Описание основного "узкого места".
 * @param performanceSpikes Список операций с аномально долгим временем выполнения.
 * @param recommendations   Предложения по оптимизации.
 */
@Schema(description = "Отчет об анализе распределенного трейса")
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraceAnalysisReport(
        @Schema(description = "Краткое резюме анализа")
        String summary,
        @Schema(description = "Общее время выполнения запроса (мс)")
        long totalDuration,
        @Schema(description = "Описание основного 'узкого места'")
        Bottleneck bottleneck,
        @Schema(description = "Список других аномально долгих операций")
        List<PerformanceSpike> performanceSpikes,
        @Schema(description = "Рекомендации по оптимизации")
        List<String> recommendations
) {
    /**
     * Описание "узкого места".
     *
     * @param operationName     Имя самой долгой операции.
     * @param duration          Ее длительность.
     * @param percentageOfTotal Процент от общего времени выполнения.
     */
    @Schema(description = "Описание основного 'узкого места'")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bottleneck(
            String operationName,
            long duration,
            double percentageOfTotal
    ) {
    }

    /**
     * Описание аномального всплеска производительности.
     *
     * @param operationName Имя операции.
     * @param duration      Длительность.
     */
    @Schema(description = "Описание аномального всплеска производительности")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PerformanceSpike(
            String operationName,
            long duration
    ) {
    }
}