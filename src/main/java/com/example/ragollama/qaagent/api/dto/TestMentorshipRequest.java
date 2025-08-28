package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на менторское ревью автотеста.
 *
 * @param requirementsText Исходные требования, которые должен проверять тест.
 * @param testCode         Полный исходный код Java-класса с тестом для анализа.
 */
@Schema(description = "DTO для запроса на менторское ревью автотеста")
public record TestMentorshipRequest(
        @Schema(description = "Исходные требования, которые должен проверять тест", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Length(min = 20)
        String requirementsText,

        @Schema(description = "Исходный код Java-теста для анализа", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Length(min = 50)
        String testCode
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "requirementsText", this.requirementsText,
                "testCode", this.testCode
        ));
    }
}
