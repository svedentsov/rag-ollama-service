package com.example.ragollama.agent.testanalysis.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на рефакторинг кода автотеста.
 *
 * @param testCode Полный исходный код Java-класса с тестом в виде строки.
 */
@Schema(description = "DTO для запроса на рефакторинг автотеста")
public record TestRefactoringRequest(
        @Schema(description = "Исходный код Java-теста для анализа и рефакторинга", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Код теста не может быть пустым")
        @Length(min = 50, message = "Код теста слишком короткий для анализа")
        String testCode
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер агентов.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("testCode", this.testCode));
    }
}
