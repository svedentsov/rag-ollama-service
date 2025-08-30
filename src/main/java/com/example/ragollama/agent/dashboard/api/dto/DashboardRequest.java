package com.example.ragollama.agent.dashboard.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO для запроса на генерацию сводного QA-дашборда.
 * <p>
 * Агрегирует все данные, необходимые для запуска полного набора
 * аналитических конвейеров.
 *
 * @param oldRef              Исходная Git-ссылка для анализа (например, 'main').
 * @param newRef              Конечная Git-ссылка для анализа (например, 'feature/new-logic').
 * @param jacocoReportContent Содержимое JaCoCo XML отчета, соответствующее `newRef`.
 */
@Schema(description = "DTO для запроса на генерацию сводного QA-дашборда")
public record DashboardRequest(
        @Schema(description = "Исходная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/new-logic")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef,

        @Schema(description = "Содержимое JaCoCo XML отчета в виде строки", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String jacocoReportContent
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейеры.
     *
     * @return Контекст для запуска агентов.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "oldRef", oldRef,
                "newRef", newRef,
                "jacocoReportContent", jacocoReportContent,
                "days", 30 // Используем стандартный период в 30 дней для анализа техдолга
        ));
    }
}
