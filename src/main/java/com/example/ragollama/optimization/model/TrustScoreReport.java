package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для детального отчета об оценке доверия к RAG-ответу.
 * <p>
 * Этот record является явным, строго типизированным контрактом, который
 * агрегирует все компоненты, влияющие на финальную оценку.
 *
 * @param finalScore      Финальная композитная оценка доверия (0-100), рассчитанная на основе взвешенной суммы других оценок.
 * @param confidenceScore Оценка уверенности LLM-критика в ответе (0-100), полученная напрямую от языковой модели.
 * @param recencyScore    Оценка новизны использованных источников (0-100), рассчитанная детерминированно.
 * @param authorityScore  Оценка авторитетности использованных источников (0-100), рассчитанная детерминированно.
 * @param justification   Обоснование оценки от AI-критика.
 */
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
