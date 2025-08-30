package com.example.ragollama.agent.testanalysis.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO для запроса на полный анализ первопричины (RCA) падений тестов.
 * <p>
 * Агрегирует все необходимые "улики" для анализа: отчеты о тестировании,
 * Git-ссылки для получения diff и логи приложения из CI/CD.
 *
 * @param currentTestReportContent  JUnit XML отчет из текущей сборки.
 * @param baselineTestReportContent JUnit XML отчет из эталонной сборки.
 * @param oldRef                    Исходная Git-ссылка для diff.
 * @param newRef                    Конечная Git-ссылка для diff.
 * @param applicationLogs           Логи приложения, собранные во время тестового прогона.
 */
@Schema(description = "DTO для запроса на анализ первопричины падений тестов")
public record RootCauseAnalysisRequest(
        @Schema(description = "JUnit XML отчет из текущей сборки", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String currentTestReportContent,

        @Schema(description = "JUnit XML отчет из эталонной сборки (main/master)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String baselineTestReportContent,

        @Schema(description = "Исходная Git-ссылка (хэш, ветка, тег)", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка (хэш, ветка, тег)", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/new-logic")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef,

        @Schema(description = "Полные логи приложения, собранные во время тестового прогона")
        String applicationLogs
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер агентов.
     *
     * @return Контекст для запуска конвейера.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "currentTestReportContent", currentTestReportContent,
                "baselineTestReportContent", baselineTestReportContent,
                "oldRef", oldRef,
                "newRef", newRef,
                "applicationLogs", applicationLogs != null ? applicationLogs : ""
        ));
    }
}
