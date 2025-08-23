package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import org.hibernate.validator.constraints.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO для запроса на анализ расхождений (drift) OpenAPI спецификации.
 *
 * @param specUrl     URL, по которому можно загрузить спецификацию.
 * @param specContent Содержимое спецификации в виде строки (YAML или JSON).
 */
@Schema(description = "DTO для запроса на анализ расхождений (drift) OpenAPI спецификации")
public record SpecDriftAnalysisRequest(
        @Schema(description = "URL для загрузки OpenAPI спецификации", example = "https://petstore3.swagger.io/api/v3/openapi.json")
        @URL
        String specUrl,

        @Schema(description = "Содержимое OpenAPI спецификации в виде строки")
        String specContent
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
        if (specUrl != null) {
            payload.put("specUrl", specUrl);
        }
        if (specContent != null) {
            payload.put("specContent", specContent);
        }
        return new AgentContext(payload);
    }
}
