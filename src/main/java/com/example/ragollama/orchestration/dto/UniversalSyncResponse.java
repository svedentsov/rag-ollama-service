package com.example.ragollama.orchestration.dto;

import com.example.ragollama.agent.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.agent.codegeneration.api.dto.CodeGenerationResponse;
import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.chat.api.dto.ChatResponse;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO для полного, синхронного (не-потокового) ответа от оркестратора.
 * <p>
 * Этот DTO теперь включает новый фабричный метод `from(List<UniversalResponse>, ...)`
 * для корректной сборки финального ответа из потоковых частей.
 */
@Schema(description = "Универсальный DTO для полного (не-потокового) ответа")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UniversalSyncResponse(
        String answer,
        String generatedCode,
        String language,
        List<SourceCitation> sourceCitations,
        UUID sessionId,
        QueryIntent intent,
        BugAnalysisResponse bugAnalysisResponse
) {
    /**
     * Фабричный метод для создания ответа на основе RAG-результата.
     */
    public static UniversalSyncResponse from(RagQueryResponse response, QueryIntent intent) {
        return new UniversalSyncResponse(response.answer(), null, null, response.sourceCitations(), response.sessionId(), intent, null);
    }

    /**
     * Фабричный метод для создания ответа на основе Chat-результата.
     */
    public static UniversalSyncResponse from(ChatResponse response, QueryIntent intent) {
        return new UniversalSyncResponse(response.responseMessage(), null, null, null, response.sessionId(), intent, null);
    }

    /**
     * Фабричный метод для создания ответа на основе результата кодогенерации.
     */
    public static UniversalSyncResponse from(CodeGenerationResponse response, QueryIntent intent) {
        return new UniversalSyncResponse(null, response.generatedCode(), response.language(), null, null, intent, null);
    }

    /**
     * Фабричный метод для создания ответа на основе результата анализа бага.
     */
    public static UniversalSyncResponse from(BugAnalysisResponse response, QueryIntent intent) {
        return new UniversalSyncResponse(null, null, null, null, null, intent, response);
    }

    /**
     * Фабричный метод для создания ответа на основе результата саммаризации.
     */
    public static UniversalSyncResponse from(String summary, QueryIntent intent) {
        return new UniversalSyncResponse(summary, null, null, null, null, intent, null);
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

        List<SourceCitation> sources = parts.stream()
                .filter(p -> p instanceof UniversalResponse.Sources)
                .flatMap(p -> ((UniversalResponse.Sources) p).sources().stream())
                .toList();

        UniversalResponse.Code code = parts.stream()
                .filter(p -> p instanceof UniversalResponse.Code)
                .map(p -> (UniversalResponse.Code) p)
                .findFirst().orElse(null);

        BugAnalysisResponse bugAnalysis = parts.stream()
                .filter(p -> p instanceof UniversalResponse.BugAnalysis)
                .map(p -> ((UniversalResponse.BugAnalysis) p).analysis())
                .findFirst().orElse(null);

        return new UniversalSyncResponse(
                answer.isEmpty() ? null : answer,
                (code != null) ? code.generatedCode() : null,
                (code != null) ? code.language() : null,
                sources.isEmpty() ? null : sources,
                null, // Session ID is not available from stream parts
                intent,
                bugAnalysis
        );
    }
}
