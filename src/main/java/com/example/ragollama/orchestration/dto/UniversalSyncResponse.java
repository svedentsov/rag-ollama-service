package com.example.ragollama.orchestration.dto;

import com.example.ragollama.agent.api.dto.CodeGenerationResponse;
import com.example.ragollama.agent.domain.QueryIntent;
import com.example.ragollama.chat.api.dto.ChatResponse;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * DTO для полного, синхронного (не-потокового) ответа от оркестратора.
 * Агрегирует возможные ответы от всех сервисов в единую структуру.
 * Поля, имеющие значение null, не будут включены в JSON-ответ.
 *
 * @param answer          Финальный текстовый ответ (от RAG или Chat).
 * @param generatedCode   Сгенерированный код (от Code Agent).
 * @param language        Язык сгенерированного кода.
 * @param sourceCitations Список источников, использованных для RAG-ответа.
 * @param sessionId       Идентификатор сессии для продолжения диалога.
 * @param intent          Намерение, определенное Router Agent'ом для данного запроса.
 */
@Schema(description = "Универсальный DTO для полного (не-потокового) ответа")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UniversalSyncResponse(
        String answer,
        String generatedCode,
        String language,
        List<String> sourceCitations,
        UUID sessionId,
        QueryIntent intent
) {
    /**
     * Фабричный метод для создания ответа на основе RAG-результата.
     */
    public static UniversalSyncResponse from(RagQueryResponse response, QueryIntent intent) {
        return new UniversalSyncResponse(response.answer(), null, null, response.sourceCitations(), response.sessionId(), intent);
    }

    /**
     * Фабричный метод для создания ответа на основе Chat-результата.
     */
    public static UniversalSyncResponse from(ChatResponse response, QueryIntent intent) {
        return new UniversalSyncResponse(response.responseMessage(), null, null, null, response.sessionId(), intent);
    }

    /**
     * Фабричный метод для создания ответа на основе результата кодогенерации.
     */
    public static UniversalSyncResponse from(CodeGenerationResponse response, QueryIntent intent) {
        return new UniversalSyncResponse(null, response.generatedCode(), response.language(), null, null, intent);
    }
}
