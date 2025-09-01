package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "DTO для запуска симуляции гипотетического сценария")
public record SimulationRequest(
        @Schema(description = "Описание сценария на естественном языке", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Симулировать резкий рост нагрузки на RAG API до 500 RPS в течение 10 минут и проанализировать деградацию p99 latency.")
        @NotBlank
        String scenario,

        @Schema(description = "Структурированные параметры для сценария",
                example = "{\"targetService\": \"rag-api\", \"targetRps\": 500, \"durationMinutes\": 10}")
        Map<String, Object> parameters
) {
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "goal", this.scenario,
                "context", this.parameters
        ));
    }
}
