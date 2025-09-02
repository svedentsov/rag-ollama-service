package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на приоритизацию бэклога.
 *
 * @param goal               Высокоуровневая цель на следующий спринт/квартал.
 * @param analysisPeriodDays Период для анализа исторических данных в днях.
 */
@Schema(description = "DTO для запроса на приоритизацию бэклога")
public record PrioritizationRequest(
        @Schema(description = "Высокоуровневая цель на следующий спринт/квартал", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "В этом спринте мы фокусируемся на повышении стабильности и устранении критических багов.")
        @NotBlank @Length(min = 20)
        String goal,

        @Schema(description = "Период для анализа исторических данных в днях", defaultValue = "30")
        @NotNull @Min(7) @Max(180)
        Integer analysisPeriodDays
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "goal", this.goal,
                "analysisPeriodDays", this.analysisPeriodDays,
                "days", this.analysisPeriodDays // Для совместимости с некоторыми агентами
        ));
    }
}
