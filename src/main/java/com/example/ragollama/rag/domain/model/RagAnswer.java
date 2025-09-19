package com.example.ragollama.rag.domain.model;

import com.example.ragollama.evaluation.model.ValidationReport;
import com.example.ragollama.optimization.model.TrustScoreReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для полного, обогащенного ответа от RAG-системы.
 * <p>
 * Агрегирует не только сам ответ и источники, но и метаданные о его
 * качестве и надежности, такие как Оценка Доверия и отчет от AI-критика.
 *
 * @param answer           Финальный текстовый ответ.
 * @param sourceCitations  Список использованных источников.
 * @param trustScoreReport Отчет об оценке доверия.
 * @param validationReport Отчет от AI-критика о качестве.
 */
@Schema(description = "Результат работы RAG-сервиса")
public record RagAnswer(
        String answer,
        List<SourceCitation> sourceCitations,
        TrustScoreReport trustScoreReport,
        ValidationReport validationReport
) {
    /**
     * Конструктор для создания ответа без отчетов о качестве.
     */
    public RagAnswer(String answer, List<SourceCitation> sourceCitations) {
        this(answer, sourceCitations, null, null);
    }

    /**
     * Конструктор для создания ответа с отчетом о доверии.
     */
    public RagAnswer(String answer, List<SourceCitation> sourceCitations, TrustScoreReport trustScoreReport) {
        this(answer, sourceCitations, trustScoreReport, null);
    }
}