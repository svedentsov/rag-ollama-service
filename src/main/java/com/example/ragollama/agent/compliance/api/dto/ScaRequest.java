package com.example.ragollama.agent.compliance.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на анализ состава ПО (SCA).
 *
 * @param buildFileContent Содержимое файла сборки (например, build.gradle).
 * @param licensePolicy    Политика лицензирования компании на естественном языке.
 */
@Schema(description = "DTO для запроса на анализ состава ПО (SCA)")
public record ScaRequest(
        @Schema(description = "Содержимое файла сборки (build.gradle)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Length(min = 50)
        String buildFileContent,

        @Schema(description = "Политика лицензирования компании", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Разрешены лицензии: MIT, Apache 2.0. Запрещены все виды GPL и AGPL лицензий.")
        @NotBlank @Length(min = 20)
        String licensePolicy
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "buildFileContent", this.buildFileContent,
                "licensePolicy", this.licensePolicy
        ));
    }
}
