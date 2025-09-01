package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "DTO для запуска A/B-тестирования RAG-конфигураций")
public record ExperimentRequest(
        @Schema(description = "Описание цели эксперимента", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Проверка влияния увеличения topK на метрику Recall")
        @NotEmpty
        String baselineDescription,

        @Schema(description = "Карта вариантов для тестирования. Ключ - имя варианта, значение - карта с переопределениями конфигурации.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        @Size(min = 1)
        Map<String, Map<String, Object>> variants
) {
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "baselineDescription", this.baselineDescription,
                "variants", this.variants
        ));
    }
}
