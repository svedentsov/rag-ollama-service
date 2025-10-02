package com.example.ragollama.orchestration.handlers;

import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IntentHandler {

    QueryIntent canHandle();

    default QueryIntent fallbackIntent() {
        return null;
    }

    /**
     * Асинхронно обрабатывает запрос и возвращает полный, агрегированный ответ.
     * @param request Универсальный запрос от пользователя.
     * @param taskId ID асинхронной задачи.
     * @return CompletableFuture с полным ответом.
     */
    CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request, UUID taskId);

    /**
     * Асинхронно обрабатывает запрос и возвращает реактивный поток.
     * @param request Универсальный запрос от пользователя.
     * @param taskId ID асинхронной задачи.
     * @return Flux с частями ответа.
     */
    Flux<UniversalResponse> handleStream(UniversalRequest request, UUID taskId);
}
