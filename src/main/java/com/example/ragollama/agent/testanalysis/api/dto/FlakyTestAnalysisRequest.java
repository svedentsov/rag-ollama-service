package com.example.ragollama.agent.testanalysis.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO для запроса на анализ "плавающих" тестов.
 *
 * @param currentTestReportContent  Содержимое JUnit XML отчета из текущей сборки (например, на PR).
 * @param baselineTestReportContent Содержимое JUnit XML отчета из эталонной сборки (например, последний успешный на main).
 */
@Schema(description = "DTO для запроса на анализ 'плавающих' тестов")
public record FlakyTestAnalysisRequest(
        @Schema(description = "JUnit XML отчет из текущей сборки", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Отчет о текущем прогоне не может быть пустым")
        String currentTestReportContent,

        @Schema(description = "JUnit XML отчет из эталонной сборки (main/master)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Отчет об эталонном прогоне не может быть пустым")
        String baselineTestReportContent
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер агентов.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "currentTestReportContent", currentTestReportContent,
                "baselineTestReportContent", baselineTestReportContent
        ));
    }
}
