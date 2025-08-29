package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса к оркестратору принятия решений по канареечным развертываниям.
 *
 * @param metricsData    Данные метрик для анализа.
 * @param decisionPolicy Политика принятия решений на естественном языке.
 */
@Schema(description = "DTO для запроса к Canary Decision Orchestrator")
public record CanaryDecisionRequest(
        @Schema(description = "Данные метрик для сравнения")
        @NotEmpty
        Map<String, CanaryAnalysisRequest.@Valid MetricData> metricsData,

        @Schema(description = "Политика принятия решений на естественном языке", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Продвигай, если нет значительных ухудшений. Откатывай при любом ухудшении метрик ошибок или p99 задержки. В остальных случаях - продолжай наблюдение.")
        @NotBlank @Length(min = 20)
        String decisionPolicy
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "metricsData", this.metricsData,
                "decisionPolicy", this.decisionPolicy
        ));
    }
}
