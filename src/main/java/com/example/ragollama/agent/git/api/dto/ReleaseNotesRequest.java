package com.example.ragollama.agent.git.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO для запроса на генерацию заметок о выпуске.
 *
 * @param oldRef Исходная Git-ссылка (обычно тег предыдущего релиза).
 * @param newRef Конечная Git-ссылка (обычно тег текущего релиза).
 */
@Schema(description = "DTO для запроса на генерацию заметок о выпуске")
public record ReleaseNotesRequest(
        @Schema(description = "Исходная Git-ссылка (тег, ветка)", requiredMode = Schema.RequiredMode.REQUIRED, example = "v1.0.0")
        @NotBlank
        @Pattern(regexp = "^[\\w\\-./]+$", message = "Недопустимые символы в Git-ссылке")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка (тег, ветка)", requiredMode = Schema.RequiredMode.REQUIRED, example = "v1.1.0")
        @NotBlank
        @Pattern(regexp = "^[\\w\\-./]+$", message = "Недопустимые символы в Git-ссылке")
        String newRef
) {
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "oldRef", oldRef,
                "newRef", newRef
        ));
    }
}
