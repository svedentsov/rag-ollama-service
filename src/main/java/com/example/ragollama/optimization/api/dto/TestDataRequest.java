package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;
import java.util.Optional;

/**
 * DTO для высокоуровневого запроса на генерацию тестовых данных.
 *
 * @param goal    Описание требуемых данных на естественном языке.
 * @param context Опциональная карта с дополнительными параметрами,
 *                такими как `classDefinition` или `tableSchema`.
 */
@Schema(description = "DTO для запроса на генерацию тестовых данных")
public record TestDataRequest(
        @Schema(description = "Описание требуемых данных на естественном языке", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Мне нужны 5 моковых объектов для DTO класса UserDto. Код класса я приложил в контексте.")
        @NotBlank @Length(min = 20)
        String goal,

        @Schema(description = "Дополнительный контекст, например, код DTO или схема таблицы")
        Map<String, Object> context
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        Map<String, Object> payload = Optional.ofNullable(context).orElse(Map.of());
        payload.put("goal", this.goal);
        return new AgentContext(payload);
    }
}