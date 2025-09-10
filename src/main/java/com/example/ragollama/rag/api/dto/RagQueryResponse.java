package com.example.ragollama.rag.api.dto;

import com.example.ragollama.evaluation.model.ValidationReport;
import com.example.ragollama.optimization.model.TrustScoreReport;
import com.example.ragollama.rag.domain.model.SourceCitation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * DTO для полного (не-потокового) ответа на RAG-запрос.
 * <p> Этот record агрегирует все результаты работы RAG-конвейера, включая
 * сгенерированный ответ, список использованных источников и метаданные сессии.
 *
 * @param answer           Финальный текстовый ответ, сгенерированный LLM.
 * @param sourceCitations  Список структурированных цитат, подтверждающих ответ.
 * @param sessionId        Идентификатор сессии для продолжения диалога.
 * @param trustScoreReport Отчет об оценке доверия к ответу.
 * @param validationReport Отчет от AI-критика о качестве ответа.
 */
@Schema(description = "DTO ответа на RAG-запрос")
public record RagQueryResponse(
        String answer,
        List<SourceCitation> sourceCitations,
        UUID sessionId,
        TrustScoreReport trustScoreReport,
        ValidationReport validationReport
) {
}
