package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета с матрицей рисков.
 */
@Schema(description = "Отчет с матрицей рисков для изменений в релизе")
public record RiskMatrixReport(
        @Schema(description = "Резюме и стратегические рекомендации, сгенерированные LLM")
        String summary,
        @Schema(description = "Список всех измененных файлов с их оценками риска")
        List<RiskMatrixItem> items
) {
}
