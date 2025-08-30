package com.example.ragollama.agent.security.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;
import java.util.Optional;

/**
 * DTO для запроса на полный аудит безопасности.
 *
 * @param oldRef          Исходная Git-ссылка для сравнения.
 * @param newRef          Конечная Git-ссылка для сравнения.
 * @param applicationLogs Опциональные логи приложения, собранные во время тестового прогона.
 */
@Schema(description = "DTO для запроса на полный аудит безопасности")
public record SecurityScanRequest(
        @Schema(description = "Исходная Git-ссылка (хэш, ветка, тег)", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка (хэш, ветка, тег)", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/new-security-logic")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef,

        @Schema(description = "Опциональные логи приложения, собранные во время тестов")
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
