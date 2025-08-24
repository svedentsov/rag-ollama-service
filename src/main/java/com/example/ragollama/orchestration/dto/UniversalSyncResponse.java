package com.example.ragollama.orchestration.dto;

import com.example.ragollama.agent.api.dto.CodeGenerationResponse;
import com.example.ragollama.agent.domain.QueryIntent;
import com.example.ragollama.qaagent.api.dto.BugAnalysisResponse;
import com.example.ragollama.chat.api.dto.ChatResponse;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * DTO для полного, синхронного (не-потокового) ответа от оркестратора.
 * <p>
 * Добавлен новый фабричный метод для ответов от сервиса саммаризации.
 */
@Schema(description = "Универсальный DTO для полного (не-потокового) ответа")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UniversalSyncResponse(
        String answer,
        String generatedCode,
        String language,
        List<String> sourceCitations,
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
}
