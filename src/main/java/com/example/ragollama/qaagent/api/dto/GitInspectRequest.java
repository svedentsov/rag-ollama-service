package com.example.ragollama.qaagent.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO для запроса на анализ изменений в Git-репозитории.
 *
 * @param oldRef Исходная Git-ссылка (коммит, ветка или тег), от которой строим diff.
 * @param newRef Конечная Git-ссылка (коммит, ветка или тег), до которой строим diff.
 */
@Schema(description = "DTO для запроса на анализ изменений в Git")
public record GitInspectRequest(
        @Schema(description = "Исходная Git-ссылка (хэш, ветка, тег)", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank
        @Pattern(regexp = "^[\\w\\-./]+$", message = "Недопустимые символы в Git-ссылке")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка (хэш, ветка, тег)", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/new-logic")
        @NotBlank
        @Pattern(regexp = "^[\\w\\-./]+$", message = "Недопустимые символы в Git-ссылке")
        String newRef
) {
}
