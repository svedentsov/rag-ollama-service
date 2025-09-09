package com.example.ragollama.rag.api.dto;

import com.example.ragollama.evaluation.model.ValidationReport;
import com.example.ragollama.optimization.model.TrustScoreReport;
import com.example.ragollama.rag.domain.model.SourceCitation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "DTO ответа на RAG-запрос")
public record RagQueryResponse(
        String answer,
        List<SourceCitation> sourceCitations,
        UUID sessionId,
        TrustScoreReport trustScoreReport,
        ValidationReport validationReport
) {
}
