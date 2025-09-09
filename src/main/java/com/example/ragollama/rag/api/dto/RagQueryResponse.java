package com.example.ragollama.rag.api.dto;

import com.example.ragollama.optimization.model.TrustScoreReport;
import com.example.ragollama.rag.domain.model.SourceCitation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "DTO ответа на RAG-запрос")
public record RagQueryResponse(
        @Schema(description = "Сгенерированный ответ")
        String answer,

        @Schema(description = "Список структурированных цитат, использованных для ответа")
        List<SourceCitation> sourceCitations,

        @Schema(description = "ID сессии для продолжения диалога")
        UUID sessionId,

        @Schema(description = "Отчет об оценке доверия к ответу")
        TrustScoreReport trustScoreReport
) {
}
