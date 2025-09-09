package com.example.ragollama.rag.domain.model;

import com.example.ragollama.optimization.model.TrustScoreReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Результат работы RAG-сервиса")
public record RagAnswer(
        @Schema(description = "Сгенерированный ответ")
        String answer,

        @Schema(description = "Список структурированных цитат, использованных для ответа")
        List<SourceCitation> sourceCitations,

        @Schema(description = "Отчет об оценке доверия к ответу")
        TrustScoreReport trustScoreReport
) {
    /**
     * Вспомогательный конструктор для этапов, где оценка доверия еще не вычислена.
     */
    public RagAnswer(String answer, List<SourceCitation> sourceCitations) {
        this(answer, sourceCitations, null);
    }
}
