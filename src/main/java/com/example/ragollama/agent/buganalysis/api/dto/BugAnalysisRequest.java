package com.example.ragollama.agent.buganalysis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для запроса на анализ баг-репорта.
 *
 * @param draftDescription "Сырой" текст или черновик описания бага от пользователя.
 */
@Schema(description = "DTO для запроса на анализ баг-репорта")
public record BugAnalysisRequest(
        @Schema(description = "Черновик описания бага", requiredMode = Schema.RequiredMode.REQUIRED, example = "кнопка падает когда я кликаю")
        @NotBlank @Size(min = 10, max = 4096)
        String draftDescription
) {
}
