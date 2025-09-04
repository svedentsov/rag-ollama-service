package com.example.ragollama.agent.openapi.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO для запроса на анализ OpenAPI спецификации.
 *
 * <p>Использует полиморфный объект {@link OpenApiSourceRequest} для
 * явного указания источника спецификации, что делает контракт API
 * надежным и однозначным.
 *
 * @param source Источник спецификации (URL или контент).
 * @param query  Вопрос пользователя к спецификации на естественном языке.
 */
@Schema(description = "DTO для запроса на семантический анализ OpenAPI спецификации")
public record OpenApiQueryRequest(
        @Schema(description = "Источник OpenAPI спецификации (URL или контент)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid
        OpenApiSourceRequest source,

        @Schema(description = "Вопрос пользователя к спецификации", requiredMode = Schema.RequiredMode.REQUIRED, example = "Какие эндпоинты возвращают Pet DTO?")
        @NotBlank
        String query
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер агентов.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("source", source); // Передаем полиморфный объект напрямую
        return new AgentContext(payload);
    }
}
