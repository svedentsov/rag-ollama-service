package com.example.ragollama.agent.openapi.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO для запроса на анализ OpenAPI спецификации.
 * <p>
 * Позволяет передать спецификацию либо по URL, либо напрямую в виде текста.
 * Кастомный валидатор {@code isSourceProvided} гарантирует, что будет
 * предоставлен ровно один источник.
 *
 * @param specUrl     URL, по которому можно загрузить спецификацию.
 * @param specContent Содержимое спецификации в виде строки (YAML или JSON).
 * @param query       Вопрос пользователя к спецификации на естественном языке.
 */
@Schema(description = "DTO для запроса на семантический анализ OpenAPI спецификации")
public record OpenApiQueryRequest(
        @Schema(description = "URL для загрузки OpenAPI спецификации", example = "https://petstore3.swagger.io/api/v3/openapi.json")
        @URL
        String specUrl,

        @Schema(description = "Содержимое OpenAPI спецификации в виде строки")
        String specContent,

        @Schema(description = "Вопрос пользователя к спецификации", requiredMode = Schema.RequiredMode.REQUIRED, example = "Какие эндпоинты возвращают Pet DTO?")
        @NotBlank
        String query
) {
    /**
     * Валидатор, проверяющий, что предоставлен ровно один источник спецификации.
     *
     * @return {@code true}, если валидация пройдена.
     */
    @AssertTrue(message = "Необходимо указать либо specUrl, либо specContent, но не оба одновременно")
    private boolean isSourceProvided() {
        boolean urlProvided = specUrl != null && !specUrl.isBlank();
        boolean contentProvided = specContent != null && !specContent.isBlank();
        return urlProvided ^ contentProvided; // XOR
    }

    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер агентов.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        if (specUrl != null) {
            payload.put("specUrl", specUrl);
        }
        if (specContent != null) {
            payload.put("specContent", specContent);
        }
        return new AgentContext(payload);
    }
}
