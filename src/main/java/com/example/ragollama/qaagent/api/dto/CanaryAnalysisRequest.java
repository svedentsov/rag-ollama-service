package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * DTO для запроса на проведение канареечного анализа.
 *
 * @param metricsData Карта, где ключ - имя метрики, а значение - объект с данными
 *                    для baseline и canary версий.
 */
@Schema(description = "DTO для запроса на канареечный анализ")
public record CanaryAnalysisRequest(
        @Schema(description = "Данные метрик для сравнения")
        @NotEmpty
        Map<String, @Valid MetricData> metricsData
) {
    /**
     * DTO для хранения данных одной метрики.
     *
     * @param baselineValues Список значений для baseline (контрольной) версии.
     * @param canaryValues   Список значений для canary (экспериментальной) версии.
     */
    @Schema(description = "Данные одной метрики для baseline и canary")
    public record MetricData(
            @ArraySchema(schema = @Schema(type = "number"), minItems = 5, uniqueItems = false)
            @NotNull @Size(min = 5, message = "Требуется минимум 5 точек данных для baseline")
            List<Double> baselineValues,

            @ArraySchema(schema = @Schema(type = "number"), minItems = 5, uniqueItems = false)
            @NotNull @Size(min = 5, message = "Требуется минимум 5 точек данных для canary")
            List<Double> canaryValues
    ) {
    }

    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("metricsData", this.metricsData));
    }
}
