package com.example.ragollama.agent.compliance.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;
import java.util.Optional;

/**
 * DTO для запроса на сбор доказательств для аудита соответствия.
 *
 * @param oldRef          Исходная Git-ссылка (например, тег предыдущего релиза).
 * @param newRef          Конечная Git-ссылка (например, тег текущего релиза).
 * @param applicationLogs Опциональные логи приложения для анализа.
 */
@Schema(description = "DTO для запроса на сбор доказательств для аудита")
public record ComplianceEvidenceRequest(
        @Schema(description = "Исходная Git-ссылка (например, тег предыдущего релиза)", requiredMode = Schema.RequiredMode.REQUIRED, example = "v1.2.0")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка (например, тег текущего релиза)", requiredMode = Schema.RequiredMode.REQUIRED, example = "v1.3.0")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef,

        @Schema(description = "Опциональные логи приложения для анализа")
        String applicationLogs
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агентов.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "oldRef", this.oldRef,
                "newRef", this.newRef,
                "applicationLogs", Optional.ofNullable(this.applicationLogs).orElse("")
        ));
    }
}
