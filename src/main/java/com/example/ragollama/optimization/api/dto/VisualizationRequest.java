package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "DTO для запроса на генерацию визуализации")
public record VisualizationRequest(
        @Schema(description = "Тип диаграммы для генерации", requiredMode = Schema.RequiredMode.REQUIRED, example = "BAR_CHART")
        @NotNull
        ChartType chartType,

        @Schema(description = "Данные для визуализации в формате JSON", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        JsonNode data,

        @Schema(description = "Инструкция на естественном языке", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Построй гистограмму, показывающую количество элементов тестового долга каждого типа (DebtType).")
        @NotBlank
        String instruction
) {
    public enum ChartType {
        BAR_CHART, PIE_CHART, TIMESERIES
    }

    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "chartType", this.chartType.name(),
                "data", this.data,
                "instruction", this.instruction
        ));
    }
}
