package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO для запроса на экономическую оценку технического долга.
 *
 * @param filePath Путь к файлу, который необходимо проанализировать.
 * @param ref      Git-ссылка (ветка или коммит), в которой находится файл.
 */
@Schema(description = "DTO для запроса на экономическую оценку техдолга")
public record EconomicImpactRequest(
        @Schema(description = "Полный путь к файлу для анализа", requiredMode = Schema.RequiredMode.REQUIRED, example = "src/main/java/com/example/ragollama/qaagent/impl/UserService.java")
        @NotBlank
        String filePath,

        @Schema(description = "Git-ссылка, в которой находится файл", requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "main")
        @NotBlank
        String ref
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("filePath", filePath, "ref", ref));
    }
}
