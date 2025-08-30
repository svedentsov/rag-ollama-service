package com.example.ragollama.agent.architecture.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO для запроса на проведение полного архитектурного ревью.
 *
 * @param oldRef                 Исходная Git-ссылка.
 * @param newRef                 Конечная Git-ссылка.
 * @param architecturePrinciples Описание эталонной архитектуры.
 * @param privacyPolicy          Политика конфиденциальности.
 */
@Schema(description = "DTO для запроса на полный архитектурный аудит")
public record ArchitecturalGuidanceRequest(
        @Schema(description = "Исходная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/refactoring")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef,

        @Schema(description = "Описание эталонной архитектуры", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String architecturePrinciples,

        @Schema(description = "Политика конфиденциальности компании", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String privacyPolicy
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "oldRef", this.oldRef,
                "newRef", this.newRef,
                "architecturePrinciples", this.architecturePrinciples,
                "privacyPolicy", this.privacyPolicy
        ));
    }
}
