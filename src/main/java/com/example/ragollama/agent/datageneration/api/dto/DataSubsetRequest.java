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
 * DTO для запроса на создание подмножества данных.
 *
 * @param tableSchema Описание схемы таблицы в формате SQL DDL (`CREATE TABLE ...`).
 * @param goal        Описание на естественном языке, какое именно подмножество данных требуется.
 * @param recordLimit Максимальное количество записей, которое должен вернуть агент.
 */
@Schema(description = "DTO для запроса на создание подмножества данных")
public record DataSubsetRequest(
        @Schema(description = "Описание схемы таблицы в формате SQL DDL", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "CREATE TABLE users (id UUID, name TEXT, email TEXT, region TEXT, last_login TIMESTAMP);")
        @NotBlank @Length(min = 20)
        String tableSchema,

        @Schema(description = "Цель на естественном языке", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Мне нужен срез данных по 5 пользователям из каждого региона, которые логинились в последний месяц.")
        @NotBlank @Length(min = 10)
        String goal,

        @Schema(description = "Максимальное количество возвращаемых записей", defaultValue = "100")
        @NotNull @Min(1) @Max(500)
        Integer recordLimit
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "tableSchema", this.tableSchema,
                "goal", this.goal,
                "limit", this.recordLimit
        ));
    }
}
