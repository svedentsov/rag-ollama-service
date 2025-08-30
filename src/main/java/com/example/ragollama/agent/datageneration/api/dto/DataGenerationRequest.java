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
 * DTO для запроса на генерацию статистически-релевантных синтетических данных.
 *
 * @param sourceSqlQuery SQL-запрос, который выбирает исходный набор данных для профилирования.
 * @param recordCount    Количество синтетических записей, которое необходимо сгенерировать.
 */
@Schema(description = "DTO для запроса на генерацию статистически-релевантных данных")
public record DataGenerationRequest(
        @Schema(description = "SQL-запрос для выборки исходных данных для профилирования", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "SELECT region, subscription_plan, age FROM users LIMIT 1000")
        @NotBlank @Length(min = 20)
        String sourceSqlQuery,

        @Schema(description = "Количество синтетических записей для генерации", defaultValue = "100")
        @NotNull @Min(10) @Max(5000) // Ограничиваем для предотвращения слишком долгих запросов
        Integer recordCount
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "sourceSqlQuery", this.sourceSqlQuery,
                "recordCount", this.recordCount
        ));
    }
}
