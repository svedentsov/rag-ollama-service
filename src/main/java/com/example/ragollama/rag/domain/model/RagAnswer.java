package com.example.ragollama.rag.domain.model;

import com.example.ragollama.evaluation.model.ValidationReport;
import com.example.ragollama.optimization.model.TrustScoreReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Результат работы RAG-сервиса")
public record RagAnswer(
        String answer,
        List<SourceCitation> sourceCitations,
        TrustScoreReport trustScoreReport,
        ValidationReport validationReport
) {
    public RagAnswer(String answer, List<SourceCitation> sourceCitations) {
        this(answer, sourceCitations, null, null);
    }

    public RagAnswer(String answer, List<SourceCitation> sourceCitations, TrustScoreReport trustScoreReport) {
        this(answer, sourceCitations, trustScoreReport, null);
    }
}
