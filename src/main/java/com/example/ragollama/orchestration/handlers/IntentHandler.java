package com.example.ragollama.orchestration.handlers;

import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Определяет контракт для обработчика одного конкретного намерения пользователя (Intent).
 * <p>
 * Эта версия интерфейса разделяет обработку на два явных метода:
 * один для синхронных (полных) ответов и другой для потоковых.
 * Это делает архитектуру более строгой, предсказуемой и устраняет
 * проблемы с преобразованием типов.
 */
public interface IntentHandler {

    /**
     * Возвращает основное намерение, которое может обрабатывать данный компонент.
     *
     * @return {@link QueryIntent}, за который отвечает этот обработчик.
     */
    QueryIntent canHandle();

    /**
     * Возвращает резервное (fallback) намерение, которое также может быть обработано этим компонентом.
     *
     * @return Резервный {@link QueryIntent} или {@code null}, если его нет.
     */
    default QueryIntent fallbackIntent() {
        return null;
    }

    /**
     * Асинхронно обрабатывает запрос и возвращает полный, агрегированный ответ.
     *
     * @param request Универсальный запрос от пользователя.
     * @return {@link CompletableFuture} с полным ответом {@link UniversalSyncResponse}.
     */
    CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request);

    /**
     * Асинхронно обрабатывает запрос и возвращает реактивный поток частей ответа.
     *
     * @param request Универсальный запрос от пользователя.
     * @return {@link Flux} с частями ответа в формате {@link UniversalResponse}.
     */
    Flux<UniversalResponse> handleStream(UniversalRequest request);
}
