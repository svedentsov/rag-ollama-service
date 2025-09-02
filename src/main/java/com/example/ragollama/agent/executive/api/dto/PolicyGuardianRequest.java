package com.example.ragollama.agent.executive.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO для запроса на запуск "Губернатора Политик и Безопасности".
 *
 * @param oldRef                 Исходная Git-ссылка для анализа.
 * @param newRef                 Конечная Git-ссылка для анализа.
 * @param architecturePrinciples Описание эталонной архитектуры.
 * @param privacyPolicy          Политика конфиденциальности.
 * @param licensePolicy          Политика лицензирования Open-Source.
 */
@Schema(description = "DTO для запроса на запуск 'Policy & Safety Governor'")
public record PolicyGuardianRequest(
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
        String privacyPolicy,

        @Schema(description = "Политика лицензирования Open-Source", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String licensePolicy
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "oldRef", this.oldRef,
                "newRef", this.newRef,
                "architecturePrinciples", this.architecturePrinciples,
                "privacyPolicy", this.privacyPolicy,
                "licensePolicy", this.licensePolicy
        ));
    }
}
