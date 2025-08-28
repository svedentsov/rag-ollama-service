package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.model.GeneratedTestCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.util.List;
import java.util.Map;

/**
 * DTO для запроса к XAI Test Oracle.
 *
 * @param requirementsText Текстовое описание требований.
 * @param generatedTests   Список сгенерированных тестов для анализа.
 */
@Schema(description = "DTO для запроса к XAI Test Oracle")
public record TestOracleRequest(
        @Schema(description = "Исходное описание требований", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty @Length(min = 20)
        String requirementsText,

        @Schema(description = "Список сгенерированных тестов для анализа", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @NotEmpty
        List<GeneratedTestCase> generatedTests
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "requirementsText", this.requirementsText,
                "generatedTests", this.generatedTests
        ));
    }
}
