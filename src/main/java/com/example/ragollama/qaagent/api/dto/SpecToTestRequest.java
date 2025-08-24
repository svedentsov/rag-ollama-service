package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO для запроса на генерацию кода API-теста.
 *
 * @param specUrl         URL OpenAPI спецификации.
 * @param specContent     Содержимое OpenAPI спецификации в виде строки.
 * @param targetEndpoint  Идентификатор целевого эндпоинта в формате "METHOD /path".
 */
@Schema(description = "DTO для запроса на генерацию кода API-теста из спецификации")
public record SpecToTestRequest(
        @Schema(description = "URL для загрузки OpenAPI спецификации", example = "https://petstore3.swagger.io/api/v3/openapi.json")
        @URL
        String specUrl,

        @Schema(description = "Содержимое OpenAPI спецификации в виде строки")
        String specContent,

        @Schema(description = "Целевой эндпоинт для генерации теста", requiredMode = Schema.RequiredMode.REQUIRED, example = "POST /pet")
        @NotBlank
        @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS|TRACE)\\s[/\\w\\-_{}]+$", message = "Неверный формат эндпоинта. Пример: 'POST /api/v1/users'")
        String targetEndpoint
) {
    @AssertTrue(message = "Необходимо указать либо specUrl, либо specContent, но не оба одновременно")
    private boolean isSourceProvided() {
        boolean urlProvided = specUrl != null && !specUrl.isBlank();
        boolean contentProvided = specContent != null && !specContent.isBlank();
        return urlProvided ^ contentProvided;
    }
    
    public AgentContext toAgentContext() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetEndpoint", targetEndpoint);
        if (specUrl != null) {
            payload.put("specUrl", specUrl);
        }
        if (specContent != null) {
            payload.put("specContent", specContent);
        }
        return new AgentContext(payload);
    }
}
