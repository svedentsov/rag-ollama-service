package com.example.ragollama.agent.coverage.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO для запроса на аудит тестового покрытия.
 *
 * @param oldRef              Исходная Git-ссылка для сравнения.
 * @param newRef              Конечная Git-ссылка для сравнения.
 * @param jacocoReportContent Содержимое JaCoCo XML отчета в виде строки.
 */
@Schema(description = "DTO для запроса на аудит тестового покрытия")
public record CoverageAuditRequest(
        @Schema(description = "Исходная Git-ссылка (хэш, ветка, тег)", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка (хэш, ветка, тег)", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/new-logic")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef,

        @Schema(description = "Содержимое JaCoCo XML отчета", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String jacocoReportContent
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "oldRef", oldRef,
                "newRef", newRef,
                "jacocoReportContent", jacocoReportContent
        ));
    }
}
