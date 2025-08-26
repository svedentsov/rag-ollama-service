package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO для запроса на комплексную оценку готовности релиза.
 *
 * @param oldRef              Исходная Git-ссылка.
 * @param newRef              Конечная Git-ссылка.
 * @param jacocoReportContent Содержимое JaCoCo XML отчета.
 */
@Schema(description = "DTO для запроса на оценку готовности релиза")
public record ReleaseReadinessRequest(
        @Schema(description = "Исходная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/new-logic")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef,

        @Schema(description = "Содержимое JaCoCo XML отчета", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String jacocoReportContent
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска конвейера.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "oldRef", oldRef,
                "newRef", newRef,
                "jacocoReportContent", jacocoReportContent
        ));
    }
}
