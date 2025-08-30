package com.example.ragollama.agent.buganalysis.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на помощь в создании баг-репорта.
 *
 * @param rawDescription "Сырое" описание проблемы от тестировщика.
 * @param testingContext Контекст, в котором был найден баг (например, какой шаг чек-листа выполнялся).
 */
@Schema(description = "DTO для запроса на помощь в создании баг-репорта")
public record BugReportAssistanceRequest(
        @Schema(description = "Сырое описание проблемы", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Length(min = 10)
        String rawDescription,

        @Schema(description = "Контекст тестирования (например, выполняемый шаг чек-листа)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String testingContext
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "rawReportText", this.rawDescription,
                "testingContext", this.testingContext
        ));
    }
}
