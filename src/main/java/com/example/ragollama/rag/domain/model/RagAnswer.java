package com.example.ragollama.rag.domain.model;

import com.example.ragollama.evaluation.model.ValidationReport;
import com.example.ragollama.optimization.model.TrustScoreReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для полного, обогащенного ответа от RAG-системы.
 *
 * @param answer                Финальный текстовый ответ.
 * @param sourceCitations       Список использованных источников.
 * @param queryFormationHistory История трансформации исходного запроса пользователя.
 * @param finalPrompt           Полный текст промпта, отправленного в LLM.
 * @param trustScoreReport      Отчет об оценке доверия к ответу.
 * @param validationReport      Отчет от AI-критика о качестве ответа.
 */
@Schema(description = "Результат работы RAG-сервиса")
public record RagAnswer(
        String answer,
        List<SourceCitation> sourceCitations,
        List<QueryFormationStep> queryFormationHistory,
        String finalPrompt,
        TrustScoreReport trustScoreReport,
        ValidationReport validationReport
) {
    /**
     * Конструктор для создания ответа без отчетов о качестве.
     * @param answer                Финальный текстовый ответ.
     * @param sourceCitations       Список использованных источников.
     * @param queryFormationHistory История трансформации исходного запроса пользователя.
     * @param finalPrompt           Полный текст промпта, отправленного в LLM.
     */
    public RagAnswer(String answer, List<SourceCitation> sourceCitations, List<QueryFormationStep> queryFormationHistory, String finalPrompt) {
        this(answer, sourceCitations, queryFormationHistory, finalPrompt, null, null);
    }
}
