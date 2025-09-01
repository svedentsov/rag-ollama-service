package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Детальный отчет об оценке доверия к RAG-ответу")
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrustScoreReport(
        @Schema(description = "Финальная композитная оценка доверия (0-100)")
        int finalScore,
        @Schema(description = "Оценка уверенности LLM-критика в ответе (0-100)")
        int confidenceScore,
        @Schema(description = "Оценка новизны использованных источников (0-100)")
        int recencyScore,
        @Schema(description = "Оценка авторитетности использованных источников (0-100)")
        int authorityScore,
        @Schema(description = "Обоснование оценки от AI-критика")
        String justification
) {
}
