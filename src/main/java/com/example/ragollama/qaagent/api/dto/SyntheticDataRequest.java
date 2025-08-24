package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO для запроса на генерацию синтетических данных.
 *
 * @param classDefinition Исходный код Java-класса (DTO, Entity) в виде строки.
 * @param count           Количество мок-объектов, которое необходимо сгенерировать.
 */
@Schema(description = "DTO для запроса на генерацию синтетических данных")
public record SyntheticDataRequest(
        @Schema(description = "Исходный код Java-класса (DTO или Entity)", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "public record UserDto(UUID id, String name, String email) {}")
        @NotBlank(message = "Определение класса не может быть пустым")
        String classDefinition,

        @Schema(description = "Количество мок-объектов для генерации", defaultValue = "3", example = "5")
        @Min(value = 1, message = "Количество должно быть не меньше 1")
        @Max(value = 20, message = "Количество не должно превышать 20")
        Integer count
) {
    public AgentContext toAgentContext() {
        // Обеспечиваем значение по умолчанию, если count не передан
        int finalCount = (count != null) ? count : 3;
        return new AgentContext(Map.of(
                "classDefinition", classDefinition,
                "count", finalCount
        ));
    }
}
