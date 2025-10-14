package com.example.ragollama.orchestration.dto;

import com.example.ragollama.agent.buganalysis.model.BugAnalysisReport;
import com.example.ragollama.agent.codegeneration.api.dto.CodeGenerationResponse;
import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.chat.api.dto.ChatResponse;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.domain.model.QueryFormationStep;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO для полного, синхронного (не-потокового) ответа от оркестратора.
 * <p>
 * Эта версия включает новый фабричный метод `from(List<UniversalResponse>, ...)`
 * для корректной сборки финального ответа из потоковых частей.
 */
@Schema(description = "Универсальный DTO для полного (не-потокового) ответа")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UniversalSyncResponse(
        String answer,
        String generatedCode,
        String language,
        List<SourceCitation> sourceCitations,
        List<QueryFormationStep> queryFormationHistory,
        String finalPrompt,
        UUID sessionId,
        QueryIntent intent,
        BugAnalysisReport bugAnalysisResponse
) {
    /**
     * Фабричный метод для создания ответа на основе RAG-результата.
     */
    public static UniversalSyncResponse from(RagQueryResponse response, QueryIntent intent) {
        return new UniversalSyncResponse(
                response.answer(), null, null, response.sourceCitations(),
                response.queryFormationHistory(), response.finalPrompt(),
                response.sessionId(), intent, null);
    }

    /**
     * Фабричный метод для создания ответа на основе Chat-результата.
     */
    public static UniversalSyncResponse from(ChatResponse response, QueryIntent intent) {
        return new UniversalSyncResponse(
                response.responseMessage(), null, null, null,
                null, response.finalPrompt(),
                response.sessionId(), intent, null);
    }

    /**
     * Фабричный метод для создания ответа на основе результата кодогенерации.
     */
    public static UniversalSyncResponse from(CodeGenerationResponse response, QueryIntent intent) {
        return new UniversalSyncResponse(
                null, response.generatedCode(), response.language(), null,
                null, response.finalPrompt(),
                null, intent, null);
    }

    /**
     * Фабричный метод для создания ответа на основе результата анализа бага.
     */
    public static UniversalSyncResponse from(BugAnalysisReport response, QueryIntent intent) {
        return new UniversalSyncResponse(null, null, null, null, null, null, null, intent, response);
    }

    /**
     * Фабричный метод для создания ответа на основе результата саммаризации.
     */
    public static UniversalSyncResponse from(String summary, QueryIntent intent) {
        return new UniversalSyncResponse(summary, null, null, null, null, null, null, intent, null);
    }

    /**
     * Фабричный метод для агрегации частей потокового ответа в единый синхронный ответ.
     *
     * @param parts  Список частей ответа, полученных из потока.
     * @param intent Намерение, которое было обработано.
     * @return Собранный {@link UniversalSyncResponse}.
     */
    public static UniversalSyncResponse from(List<UniversalResponse> parts, QueryIntent intent) {
        String answer = parts.stream()
                .filter(p -> p instanceof UniversalResponse.Content)
                .map(p -> ((UniversalResponse.Content) p).text())
                .collect(Collectors.joining());

        UniversalResponse.Sources sourcesPart = parts.stream()
                .filter(p -> p instanceof UniversalResponse.Sources)
                .map(p -> (UniversalResponse.Sources) p)
                .findFirst().orElse(null);

        UniversalResponse.Code codePart = parts.stream()
                .filter(p -> p instanceof UniversalResponse.Code)
                .map(p -> (UniversalResponse.Code) p)
                .findFirst().orElse(null);

        BugAnalysisReport bugAnalysis = parts.stream()
                .filter(p -> p instanceof UniversalResponse.BugAnalysis)
                .map(p -> ((UniversalResponse.BugAnalysis) p).analysis())
                .findFirst().orElse(null);

        return new UniversalSyncResponse(
                answer.isEmpty() ? null : answer,
                (codePart != null) ? codePart.generatedCode() : null,
                (codePart != null) ? codePart.language() : null,
                (sourcesPart != null) ? sourcesPart.sources() : null,
                (sourcesPart != null) ? sourcesPart.queryFormationHistory() : null,
                (sourcesPart != null) ? sourcesPart.finalPrompt() : null,
                null, // Session ID is not available from stream parts
                intent,
                bugAnalysis
        );
    }
}
