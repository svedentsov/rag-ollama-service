package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * DTO для запроса на анализ дрейфа признаков.
 *
 * @param baselineData   Эталонный набор данных.
 * @param productionData Текущий (продакшен) набор данных для сравнения.
 */
@Schema(description = "DTO для запроса на анализ дрейфа признаков")
public record DriftDetectionRequest(
        @Schema(description = "Эталонный набор данных (например, из обучающей выборки)")
        @NotEmpty
        List<@Valid Map<String, Object>> baselineData,

        @Schema(description = "Текущий набор данных из продакшена для сравнения")
        @NotEmpty
        List<@Valid Map<String, Object>> productionData
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "baselineData", this.baselineData,
                "productionData", this.productionData
        ));
    }
}
