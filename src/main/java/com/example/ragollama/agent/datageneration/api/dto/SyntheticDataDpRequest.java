package com.example.ragollama.agent.datageneration.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на генерацию синтетических данных с дифференциальной приватностью.
 *
 * @param sourceSqlQuery SQL-запрос, который выбирает исходный набор данных для анализа.
 * @param recordCount    Количество синтетических записей, которое необходимо сгенерировать.
 * @param epsilon        Параметр приватности (чем меньше, тем выше приватность и больше "шума").
 */
@Schema(description = "DTO для запроса на генерацию синтетических данных (DP)")
public record SyntheticDataDpRequest(
        @Schema(description = "SQL-запрос для выборки исходных данных", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "SELECT id, name, email, region, last_login FROM users WHERE region = 'EU'")
        @NotBlank @Length(min = 20)
        String sourceSqlQuery,

        @Schema(description = "Количество синтетических записей для генерации", defaultValue = "100")
        @NotNull @Min(10) @Max(1000)
        Integer recordCount,

        @Schema(description = "Параметр приватности Epsilon (меньше = приватнее)", defaultValue = "1.0")
        @NotNull
        Double epsilon
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "sourceSqlQuery", this.sourceSqlQuery,
                "recordCount", this.recordCount,
                "epsilon", this.epsilon
        ));
    }
}
