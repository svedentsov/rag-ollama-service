package com.example.ragollama.rag.api.dto;

import com.example.ragollama.evaluation.model.ValidationReport;
import com.example.ragollama.optimization.model.TrustScoreReport;
import com.example.ragollama.rag.domain.model.QueryFormationStep;
import com.example.ragollama.rag.domain.model.SourceCitation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * DTO для полного (не-потокового) ответа на RAG-запрос.
 *
 * @param answer                Финальный текстовый ответ, сгенерированный LLM.
 * @param sourceCitations       Список структурированных цитат, подтверждающих ответ.
 * @param sessionId             Идентификатор сессии для продолжения диалога.
 * @param queryFormationHistory Детальная история шагов обработки исходного запроса.
 * @param finalPrompt           Полный текст промпта, отправленного в LLM.
 * @param trustScoreReport      Отчет об оценке доверия к ответу.
 * @param validationReport      Отчет от AI-критика о качестве ответа.
 */
@Schema(description = "DTO ответа на RAG-запрос")
public record RagQueryResponse(
        String answer,
        List<SourceCitation> sourceCitations,
        UUID sessionId,
        List<QueryFormationStep> queryFormationHistory,
        String finalPrompt,
        TrustScoreReport trustScoreReport,
        ValidationReport validationReport
) {
}
